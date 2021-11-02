package music.handlers;

import api.TraqApi;
import app.App;
import app.Responder;
import com.github.motoki317.traq4j.model.Message;
import com.github.motoki317.traq4j.model.User;
import com.github.motoki317.traq_ws_bot.WebRTCState;
import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import db.model.musicInterruptedChannel.MusicInterruptedChannel;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicInterruptedChannelRepository;
import db.repository.base.MusicQueueRepository;
import db.repository.base.MusicSettingRepository;
import http.MusicServer;
import log.Logger;
import music.*;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;
import org.apache.commons.lang.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skyway.SkywayApi;
import update.response.Response;
import update.response.ResponseManager;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static commands.BotCommand.respond;
import static commands.BotCommand.respondError;
import static music.MusicUtils.formatLength;
import static music.MusicUtils.getVoiceChannelByID;

public class MusicPlayHandler {
    private static final UUID NullUUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    // Channel UUID to Music State
    private final Map<String, MusicState> states;
    private final AudioPlayerManager playerManager;
    private final String botUserId;

    private final App app;
    private final MusicServer musicServer;
    private final TraqApi traqApi;
    private final SkywayApi skywayApi;
    private final Logger logger;
    private final MusicSettingRepository musicSettingRepository;
    private final MusicQueueRepository musicQueueRepository;
    private final MusicInterruptedChannelRepository interruptedGuildRepository;
    private final ResponseManager responseManager;

    public MusicPlayHandler(App app, Map<String, MusicState> states, AudioPlayerManager playerManager) {
        this.states = states;
        this.playerManager = playerManager;
        this.botUserId = System.getenv("BOT_USER_ID");
        this.app = app;
        this.musicServer = app.getMusicServer();
        this.traqApi = app.getTraqApi();
        this.skywayApi = app.getSkywayApi();
        this.logger = app.getLogger();
        this.musicSettingRepository = app.getDatabase().getMusicSettingRepository();
        this.musicQueueRepository = app.getDatabase().getMusicQueueRepository();
        this.interruptedGuildRepository = app.getDatabase().getMusicInterruptedChannelRepository();
        this.responseManager = app.getResponseManager();
    }

    /**
     * Retrieves voice channel the user is in.
     *
     * @param userId User UUID
     * @return Channel UUID. null if not found.
     */
    @Nullable
    private QallState getVoiceChannel(@NotNull UUID userId) {
        return MusicUtils.getVoiceChannel(this.traqApi, userId);
    }

    /**
     * Get music setting for the channel.
     *
     * @param channelId Channel ID.
     * @return Music setting. Default setting if not found.
     */
    @NotNull
    private MusicSetting getSetting(String channelId) {
        MusicSetting setting = this.musicSettingRepository.findOne(() -> channelId);
        if (setting != null) {
            return setting;
        }
        return MusicSetting.getDefault(channelId);
    }

    /**
     * Rejoin to all interrupted guilds.
     */
    public void rejoinInterruptedGuilds() {
        List<MusicInterruptedChannel> channels = this.interruptedGuildRepository.findAll();
        if (channels == null) {
            return;
        }
        this.interruptedGuildRepository.deleteAll();

        for (MusicInterruptedChannel channel : channels) {
            UUID vcId = UUID.fromString(channel.getVcId());
            MusicState state = prepareMusicState(vcId, UUID.fromString(channel.getTextChannelId()));

            // setup music server
            String next = this.musicServer.serve(
                    this.botUserId,
                    state.getPlayer(),
                    vcId
            );
            // connect with skyway
            this.skywayApi.joinChannel(next, vcId);
            // Sync WebRTC state with traQ server
            QallState qs = getVoiceChannelByID(traqApi, vcId);
            String sessionId = qs == null || qs.sessionId().equals("") ? newQallSessionId() : qs.sessionId();
            this.app.sendWebRTCState(vcId.toString(), new WebRTCState("joined", sessionId));
        }
    }

    /**
     * Prepares music state for the channel.
     * (Sets up audio players, but does not join the VC)
     *
     * @param vcId          Voice channel id.
     * @param textChannelId Text channel id.
     * @return Music state
     */
    @NotNull
    private MusicState prepareMusicState(@NotNull UUID vcId, @NotNull UUID textChannelId) {
        MusicSetting setting = getSetting(vcId.toString());
        TraqApi traqApi = this.traqApi;

        AudioPlayer player = playerManager.createPlayer();
        player.setVolume(setting.getVolume());

        TrackScheduler scheduler = new TrackScheduler(new TrackScheduler.SchedulerGateway() {
            @Override
            public void sendMessage(String message) {
                traqApi.sendMessage(textChannelId, message, true);
            }

            @Override
            public @NotNull MusicSetting getSetting() {
                return setting;
            }

            @Nullable
            @Override
            public User getUser(UUID userId) {
                return traqApi.getUserByID(userId);
            }

            @Override
            public void setLastInteract() {
                MusicState state;
                synchronized (states) {
                    state = states.getOrDefault(vcId.toString(), null);
                }
                if (state != null) {
                    state.setLastInteract(System.currentTimeMillis());
                }
            }

            @Override
            public void playTrack(AudioTrack track) {
                player.playTrack(track);
            }
        });
        player.addListener(scheduler);

        MusicState state = new MusicState(player, scheduler, setting, vcId.toString(), textChannelId.toString());
        synchronized (states) {
            states.put(vcId.toString(), state);
        }

        this.logger.log(String.format("Preparing music player (%s) for voice channel %s, text channel %s",
                states.size(), vcId, textChannelId
        ));

        enqueueSavedQueue(vcId, textChannelId, state);
        return state;
    }

    /**
     * Enqueues all saved previous queue.
     *
     * @param vcId          Voice channel ID.
     * @param textChannelId Text channel ID.
     * @param state         State.
     */
    private void enqueueSavedQueue(UUID vcId, UUID textChannelId, MusicState state) {
        List<MusicQueueEntry> queue = this.musicQueueRepository.getGuildMusicQueue(vcId.toString());
        boolean deleteRes = this.musicQueueRepository.deleteGuildMusicQueue(vcId.toString());
        if (queue == null || queue.isEmpty() || !deleteRes) {
            return;
        }

        List<Future<Void>> futures = new ArrayList<>(queue.size());

        String firstURL = queue.get(0).getUrl();
        long position = queue.get(0).getPosition();

        Map<String, MusicQueueEntry> urlMap = queue.stream().collect(Collectors.toMap(MusicQueueEntry::getUrl, q -> q));

        for (MusicQueueEntry e : queue) {
            Future<Void> f = playerManager.loadItemOrdered(state.getPlayer(), e.getUrl(), new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    // If it's the first one, set the position
                    AudioTrackInfo info = audioTrack.getInfo();
                    if (info.uri.equals(firstURL)) {
                        audioTrack.setPosition(position);
                    }
                    MusicQueueEntry e = urlMap.getOrDefault(info.uri, null);
                    try {
                        state.enqueue(new QueueEntry(audioTrack, e != null ? e.getUserId() : NullUUID));
                    } catch (DuplicateTrackException | QueueFullException ex) {
                        ex.printStackTrace();
                    }
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    // Should probably not be reached because we're supplying a URL for each song
                    for (AudioTrack track : audioPlaylist.getTracks()) {
                        try {
                            AudioTrackInfo info = track.getInfo();
                            MusicQueueEntry e = urlMap.getOrDefault(info.uri, null);
                            state.enqueue(new QueueEntry(track, e != null ? e.getUserId() : NullUUID));
                        } catch (DuplicateTrackException | QueueFullException ex) {
                            ex.printStackTrace();
                        }
                    }
                }

                @Override
                public void noMatches() {
                    MusicPlayHandler.this.logger.debug("Music: Loading old queue: No match for URL " + e.getUrl());
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    respond(traqApi, textChannelId, "Track load failed: " + e.getMessage());
                }
            });
            futures.add(f);
        }

        // Use asynchronous logic to cancel loading in case the user wants it
        String desc = String.format("Loading `%s` song%s from the previous queue...",
                queue.size(), queue.size() == 1 ? "" : "s");

        CompletableFuture<Void> all = CompletableFuture.runAsync(() -> {
            for (Future<Void> f : futures) {
                try {
                    f.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        });

        respond(traqApi, textChannelId,
                desc + "\nThis might take a while. `m purge` or `m clear` to stop loading.",
                message -> sendFinishEnqueueSaved(message, state, futures, all, desc)
        );
    }

    private void sendFinishEnqueueSaved(Message message, MusicState state, List<Future<Void>> futures,
                                        CompletableFuture<Void> all, String desc) {
        all.thenRun(() -> this.traqApi.editMessage(message.getId(), desc + "\nFinished loading!"));

        state.setOnStopLoadingCache(() -> {
            if (all.isDone()) {
                return;
            }
            all.cancel(false);
            futures.forEach(f -> {
                if (f.isDone()) return;
                f.cancel(false);
            });
            this.traqApi.editMessage(message.getId(), desc + "\nCancelled loading.");
        });
    }

    /**
     * Tries to connect to the VC the user is in.
     *
     * @param event Event.
     * @return {@code true} if success.
     */
    private boolean connect(MessageCreatedEvent event) {
        QallState qs = getVoiceChannel(UUID.fromString(event.message().user().id()));
        if (qs == null) {
            respond(this.traqApi, event,
                    "Please join in a voice channel before you use this command!");
            return false;
        }

        // Prepare whole music logic state
        MusicState state = prepareMusicState(qs.channelId(), UUID.fromString(event.message().channelId()));

        // setup music player
        String next = this.musicServer.serve(
                this.botUserId,
                state.getPlayer(),
                qs.channelId()
        );
        // connect with skyway
        this.skywayApi.joinChannel(next, qs.channelId());
        // Sync WebRTC state with traQ server
        String sessionId = qs.sessionId().equals("") ? newQallSessionId() : qs.sessionId();
        this.app.sendWebRTCState(qs.channelId().toString(), new WebRTCState("joined", sessionId));

        return true;
    }

    /**
     * Generates a new random qall session id.
     * @return New Qall session id.
     */
    private static String newQallSessionId() {
        return "qall-" + RandomStringUtils.randomAlphanumeric(10);
    }

    /**
     * Retrieves music state if the channel already has a music player set up,
     * or creates a new state if the channel doesn't.
     *
     * @param event Event.
     * @return Music state. null if failed to join in a vc.
     */
    @Nullable
    private MusicState getStateOrConnect(MessageCreatedEvent event) {
        QallState qs = getVoiceChannel(UUID.fromString(event.message().user().id()));
        if (qs == null) {
            return null;
        }

        synchronized (states) {
            MusicState state = states.getOrDefault(qs.channelId().toString(), null);
            if (state != null) {
                return state;
            }
            boolean res = connect(event);
            if (!res) {
                return null;
            }
            return states.get(qs.channelId().toString());
        }
    }

    /**
     * Handles "join" command.
     *
     * @param event Event.
     */
    public void handleJoin(MessageCreatedEvent event, Responder res) {
        synchronized (states) {
            if (states.containsKey(event.message().channelId())) {
                respond(this.traqApi, event, "This channel already has a music player set up!");
                return;
            }
        }

        if (!connect(event)) {
            return;
        }

        respond(res, "Successfully connected to your voice channel!");
    }

    /**
     * handles "leave" command.
     *
     * @param event     Event.
     * @param res       Responder.
     * @param saveQueue {@code true} if the bot should save the current queue, and use it next time.
     */
    public void handleLeave(@NotNull MessageCreatedEvent event, Responder res, boolean saveQueue) {
        QallState qs = getVoiceChannel(UUID.fromString(event.message().user().id()));
        if (qs == null) {
            respond(res, "You're not in a voice channel.");
            return;
        }

        MusicState state;
        synchronized (states) {
            state = states.getOrDefault(qs.channelId().toString(), null);
        }
        if (state == null) {
            respond(res, "This channel doesn't seem to have a music player set up.");
            return;
        }

        try {
            this.shutdownPlayer(saveQueue, qs.channelId(), state);
        } catch (RuntimeException e) {
            respondError(res, e.getMessage());
            return;
        }

        synchronized (states) {
            states.remove(qs.channelId().toString());
        }

        respond(res, String.format("Player stopped. (%s)",
                saveQueue ? "Saved the queue" : "Queue cleared"));
    }

    /**
     * Shuts down the music player for the guild.
     *
     * @param saveQueue If the bot should save the current queue.
     * @param vcId      Voice channel ID.
     * @param state     Music state.
     * @throws RuntimeException If something went wrong.
     */
    void shutdownPlayer(boolean saveQueue, UUID vcId, MusicState state) throws RuntimeException {
        // Stop loading from cache if it was running
        state.stopLoadingCache();

        // Retrieve the current queue before it is cleared
        QueueState queue = state.getCurrentQueue();

        // Stop playing, and destroy LavaPlayer audio player (this call clears the queue inside MusicState)
        state.stopPlaying();
        state.getPlayer().destroy();

        // Stop music server
        this.musicServer.stop(vcId);
        // Try to disconnect from the voice channel
        this.skywayApi.close(vcId);
        // Sync WebRTC state with traQ server
        this.app.sendWebRTCState(vcId.toString());

        // Save the current queue
        boolean saveResult = !saveQueue || saveQueue(vcId, queue);
        if (!saveResult) {
            throw new RuntimeException("Something went wrong while saving the queue...");
        }

        // Save setting if the current differs from the default
        boolean saveSetting = true;
        if (!state.getSetting().equals(MusicSetting.getDefault(vcId.toString()))) {
            saveSetting = this.musicSettingRepository.exists(vcId::toString)
                    ? this.musicSettingRepository.update(state.getSetting())
                    : this.musicSettingRepository.create(state.getSetting());
        }
        if (!saveSetting) {
            throw new RuntimeException("Something went wrong while saving settings for this guild...");
        }
    }

    /**
     * Saves the current queue.
     *
     * @param vcId  Voice channel ID.
     * @param queue Music queue.
     * @return {@code true} if success.
     */
    private boolean saveQueue(UUID vcId, @NotNull QueueState queue) {
        List<QueueEntry> tracks = new ArrayList<>(queue.getQueue());
        if (tracks.isEmpty()) {
            return true;
        }

        List<MusicQueueEntry> toSave = new ArrayList<>(tracks.size());
        long now = System.currentTimeMillis();
        for (int i = 0; i < tracks.size(); i++) {
            QueueEntry track = tracks.get(i);
            toSave.add(new MusicQueueEntry(
                    vcId.toString(),
                    i,
                    track.getUserId(),
                    track.getTrack().getInfo().uri,
                    i == 0 ? queue.getPosition() : 0L,
                    new Date(now)
            ));
        }
        return this.musicQueueRepository.saveGuildMusicQueue(toSave);
    }

    private static boolean isURL(@NotNull String possibleURL) {
        return possibleURL.startsWith("http://") || possibleURL.startsWith("https://");
    }

    /**
     * Handles "play" command.
     *
     * @param event   Event.
     * @param res     Responder.
     * @param args    Command arguments.
     * @param playAll If the bot should enqueue all the search results directly.
     * @param site    Which site to search from.
     */
    public void handlePlay(MessageCreatedEvent event, Responder res, String[] args, boolean playAll, SearchSite site) {
        if (args.length <= 2) {
            respond(res, "Please input a keyword or URL you want to search or play!");
            return;
        }

        String input = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        if (isURL(input)) {
            playAll = true;
        } else {
            input = site.getPrefix() + input;
        }

        MusicState state = getStateOrConnect(event);
        if (state == null) {
            return;
        }

        boolean finalPlayAll = playAll;
        playerManager.loadItem(input, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                enqueueSong(event, res, state, audioTrack);
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                if (audioPlaylist.getTracks().isEmpty()) {
                    respond(res, "Received an empty play list.");
                    return;
                }

                if (audioPlaylist.getTracks().size() == 1) {
                    enqueueSong(event, res, state, audioPlaylist.getTracks().get(0));
                    return;
                }

                if (!audioPlaylist.isSearchResult() || finalPlayAll) {
                    enqueueMultipleSongs(event, res, state, audioPlaylist.getTracks());
                    return;
                }

                selectSong(event, res, state, audioPlaylist);
            }

            @Override
            public void noMatches() {
                respond(res, "No results found.");
            }

            @Override
            public void loadFailed(FriendlyException e) {
                respond(res, "Something went wrong while loading tracks:\n" + e.getMessage());
            }
        });
    }

    private void selectSong(MessageCreatedEvent event, Responder res, MusicState state, AudioPlaylist playlist) {
        List<AudioTrack> tracks = playlist.getTracks();
        List<String> desc = new ArrayList<>();
        int max = Math.min(10, tracks.size());
        for (int i = 0; i < max; i++) {
            AudioTrack track = tracks.get(i);
            AudioTrackInfo info = track.getInfo();
            desc.add(String.format("%s. %s `[%s]`",
                    i + 1,
                    info.title,
                    info.isStream ? "LIVE" : formatLength(info.length)));
        }
        desc.add("");
        desc.add(String.format("all: Play all (%s songs)", tracks.size()));
        desc.add("");
        desc.add("c: Cancel");

        String msg = String.format("""
                        %s, Select a song!
                        %s
                        Type '1' ~ '%s', 'all' to play all, or 'c' to cancel.""",
                event.message().user().name(),
                String.join("\n", desc),
                max);
        UUID channelId = UUID.fromString(event.message().channelId());
        respond(this.traqApi, channelId, msg, message -> {
            UUID userId = UUID.fromString(event.message().user().id());
            Response handler = new Response(channelId, userId,
                    response -> chooseTrack(event, res, state, tracks, response)
            );
            this.responseManager.addEventListener(handler);
        });
    }

    private boolean chooseTrack(MessageCreatedEvent event, Responder res,
                                MusicState state, List<AudioTrack> tracks, MessageCreatedEvent response) {
        String msg = response.message().plainText();
        if ("all".equalsIgnoreCase(msg)) {
            this.enqueueMultipleSongs(event, res, state, tracks);
            return true;
        }
        if ("c".equalsIgnoreCase(msg)) {
            respond(res, "Cancelled.");
            return true;
        }

        try {
            int max = Math.min(10, tracks.size());
            int index = Integer.parseInt(msg);
            if (index < 1 || max < index) {
                respond(res, String.format("Please input a number between 1 and %s!", max));
                return false;
            }
            this.enqueueSong(event, res, state, tracks.get(index - 1));
            return true;
        } catch (NumberFormatException ignored) {
            // Ignore normal messages
            return false;
        }
    }

    private void enqueueSong(MessageCreatedEvent event, Responder res, MusicState state, AudioTrack audioTrack) {
        UUID userId = UUID.fromString(event.message().user().id());
        boolean toShowQueuedMsg = !state.getCurrentQueue().getQueue().isEmpty();
        int queueSize = state.getCurrentQueue().getQueue().size();
        long remainingLength = state.getRemainingLength();

        try {
            state.enqueue(new QueueEntry(audioTrack, userId));
        } catch (DuplicateTrackException | QueueFullException e) {
            respond(res, e.getMessage());
            return;
        }

        if (toShowQueuedMsg) {
            AudioTrackInfo info = audioTrack.getInfo();
            respond(res, String.format("""
                            ✔ Queued 1 song.
                            [%s](%s)
                            Length: %s
                            Position in queue: %s
                            Estimated time until playing: %s""",
                    "".equals(info.title) ? "(No title)" : info.title, info.uri,
                    info.isStream ? "LIVE" : formatLength(info.length),
                    queueSize,
                    formatLength(remainingLength)));
        }
    }

    private void enqueueMultipleSongs(MessageCreatedEvent event, Responder res, MusicState state, List<AudioTrack> tracks) {
        UUID userId = UUID.fromString(event.message().user().id());
        long remainingLength = state.getRemainingLength();
        int queueSize = state.getCurrentQueue().getQueue().size();

        int success = 0;
        long queuedLength = 0;
        for (AudioTrack track : tracks) {
            try {
                state.enqueue(new QueueEntry(track, userId));
                success++;
                queuedLength += track.getDuration();
            } catch (DuplicateTrackException | QueueFullException ignored) {
            }
        }

        respond(res, String.format("""
                        ✔ Queued %s song%s.
                        Length: %s
                        Position in queue: %s
                        Estimated time until playing: %s""",
                success, success == 1 ? "" : "s",
                formatLength(queuedLength),
                queueSize,
                formatLength(remainingLength)));

        if (success != tracks.size()) {
            respond(res, String.format("Failed to queue %s song%s due to duplicated track(s) or queue being full.",
                    tracks.size() - success, (tracks.size() - success) == 1 ? "" : "s"));
        }
    }
}
