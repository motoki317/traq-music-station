package music;

import api.TraqApi;
import app.App;
import app.Responder;
import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import commands.ChannelCommand;
import heartbeat.HeartBeatTask;
import log.Logger;
import music.handlers.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Music extends ChannelCommand {
    private static final Map<String, MusicState> states;
    private static final AudioPlayerManager playerManager;

    static {
        states = new HashMap<>();
        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private final Map<String, MusicSubCommandHandler> commands;

    // Command handlers
    private final MusicPlayHandler playHandler;
    private final MusicManagementHandler managementHandler;
    private final MusicSettingHandler settingHandler;

    // Music related heartbeat
    private final HeartBeatTask heartbeat;

    private final TraqApi traqApi;
    private final Logger logger;

    public Music(App app) {
        this.commands = new HashMap<>();
        this.traqApi = app.getTraqApi();
        this.logger = app.getLogger();
        this.playHandler = new MusicPlayHandler(app, states, playerManager);
        this.managementHandler = new MusicManagementHandler(app);
        this.settingHandler = new MusicSettingHandler(app, states);

        this.registerCommands();

        // Rejoin interrupted guilds on shutdown
        this.playHandler.rejoinInterruptedGuilds();

        // Register music related heartbeat
        this.logger.debug("Starting music heartbeat...");
        MusicAutoLeaveChecker autoLeaveChecker = new MusicAutoLeaveChecker(app, states, this.playHandler);
        this.heartbeat = new HeartBeatTask(app.getLogger(), new MusicHeartBeat(app, autoLeaveChecker));
        this.heartbeat.start();

        // Save all players on shutdown to be re-joined above
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            this.logger.log("Stopping music heartbeat and clearing up...");
            this.heartbeat.clearUp();
            autoLeaveChecker.forceShutdownAllGuilds();
            this.logger.log("Cleared up music feature!");
        }));
    }

    /**
     * Retrieves voice channel the user is in.
     * @param userId User UUID
     * @return Channel UUID. null if not found.
     */
    @Nullable
    private QallState getVoiceChannel(@NotNull UUID userId) {
        return MusicUtils.getVoiceChannel(this.traqApi, userId);
    }


    private interface MusicSubCommandRequireMusicState {
        void handle(MessageCreatedEvent event, Responder res, String[] args, UUID vcId, MusicState state);
    }

    private MusicSubCommandHandler requireMusicState(MusicSubCommandRequireMusicState handle) {
        return (event, res, args) -> {
            QallState qs = Music.this.getVoiceChannel(UUID.fromString(event.message().user().id()));
            if (qs == null) {
                respond(res, "You do not seem to be in a voice channel...");
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

            handle.handle(event, res, args, qs.channelId(), state);
        };
    }

    @SuppressWarnings("OverlyLongMethod")
    private void registerCommands() {
        // Join and leave handlers
        commands.put("j", (event, res, args) -> this.playHandler.handleJoin(event, res));
        commands.put("join", (event, res, args) -> this.playHandler.handleJoin(event, res));

        commands.put("l", (event, res, args) -> this.playHandler.handleLeave(event, res, true));
        commands.put("leave", (event, res, args) -> this.playHandler.handleLeave(event, res, true));
        commands.put("stop", (event, res, args) -> this.playHandler.handleLeave(event, res, true));

        commands.put("c", (event, res, args) -> this.playHandler.handleLeave(event, res, false));
        commands.put("clear", (event, res, args) -> this.playHandler.handleLeave(event, res, false));

        // Play handlers
        commands.put("p", (event, res, args) -> this.playHandler.handlePlay(event, res, args, false, SearchSite.YouTube));
        commands.put("play", (event, res, args) -> this.playHandler.handlePlay(event, res, args, false, SearchSite.YouTube));

        commands.put("pa", (event, res, args) -> this.playHandler.handlePlay(event, res, args, true, SearchSite.YouTube));
        commands.put("playall", (event, res, args) -> this.playHandler.handlePlay(event, res, args, true, SearchSite.YouTube));

        commands.put("sc", (event, res, args) -> this.playHandler.handlePlay(event, res, args, false, SearchSite.SoundCloud));
        commands.put("soundcloud", (event, res, args) -> this.playHandler.handlePlay(event, res, args, false, SearchSite.SoundCloud));

        // Player management handlers
        commands.put("np", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleNowPlaying(res, state)
        ));
        commands.put("nowplaying", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleNowPlaying(res, state)
        ));

        commands.put("q", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleQueue(res, args, state)
        ));
        commands.put("queue", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleQueue(res, args, state)
        ));

        commands.put("pause", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handlePause(res, state, true)
        ));

        commands.put("resume", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handlePause(res, state, false)
        ));

        commands.put("s", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleSkip(res, args, state)
        ));
        commands.put("skip", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleSkip(res, args, state)
        ));

        commands.put("seek", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleSeek(res, args, state)
        ));

        commands.put("shuffle", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handleShuffle(res, state)
        ));

        commands.put("purge", requireMusicState(
                (event, res, args, vcId, state) -> this.managementHandler.handlePurge(res, state)
        ));

        // Setting handlers
        commands.put("v", this.settingHandler.handleVolume);
        commands.put("vol", this.settingHandler.handleVolume);
        commands.put("volume", this.settingHandler.handleVolume);

        commands.put("r", this.settingHandler.handleRepeat);
        commands.put("repeat", this.settingHandler.handleRepeat);

        commands.put("setting", this.settingHandler.handleSetting);
        commands.put("settings", this.settingHandler.handleSetting);
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"m", "music"}};
    }

    @Override
    public @NotNull String syntax() {
        return "music";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Music commands! `music help` for more.";
    }

    @Override
    public @NotNull String longHelp() {
        return String.join("\n",
                "## ♪ Music Help",
                "### Join and Leave",
                "**m join** : Joins the voice channel.",
                "**m leave** : Leaves the voice channel (saves the current queue).",
                "**m clear** : Leaves the voice channel (does NOT save the current queue).",
                "",
                "### Play Songs",
                "**m play <keyword / URL>** : Searches and plays from the keyword / URL.",
                "**m playAll <keyword / URL>** : Plays all search results from the keyword / URL.",
                "**m soundcloud <keyword / URL>** : Searches SoundCloud with keyword / URL.",
                "",
                "### Player Management",
                "**m nowPlaying** : Shows the current song.",
                "**m queue** : Shows the queue.",
                "**m pause** : Pauses the song.",
                "**m resume** : Resumes the song.",
                "**m skip [num]** : Skips the current song. Append a number to skip multiple songs at once.",
                "**m seek <time>** : Seeks the current song to the specified time. e.g. `m seek 1:50`",
                "**m shuffle** : Shuffles the queue.",
                "**m purge** : Purges all waiting songs in the queue. Does not stop the current song.",
                "",
                "### Settings",
                "**m volume <percentage>** : Sets the volume. e.g. `m volume 50`",
                "**m repeat <mode>** : Sets the repeat mode.",
                "**m settings** : Other settings. Settings will be saved per guild if changed."
        );
    }

    @Override
    public void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(res, this.longHelp());
            return;
        }

        MusicSubCommandHandler handler = this.commands.getOrDefault(
                // case insensitive sub commands
                args[1].toLowerCase(),
                null
        );
        if (handler != null) {
            handler.handle(event, res, args);
            return;
        }

        respond(res, "Unknown music command. Try `m help`!");
    }
}
