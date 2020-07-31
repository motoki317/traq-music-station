package music.handlers;

import api.TraqApi;
import app.Bot;
import com.github.motoki317.traq4j.model.User;
import com.github.motoki317.traq_bot.Responder;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import music.*;

import java.util.ArrayList;
import java.util.List;

import static commands.BotCommand.respond;
import static music.MusicUtils.formatLength;
import static music.MusicUtils.parseLength;

public class MusicManagementHandler {
    private final TraqApi traqApi;

    public MusicManagementHandler(Bot bot) {
        this.traqApi = bot.getTraqApi();
    }

    /**
     * Handles "nowPlaying" command.
     * @param res Responder.
     * @param state Music state.
     */
    public void handleNowPlaying(Responder res, MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        List<QueueEntry> entries = queueState.getQueue();
        if (entries.isEmpty()) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        QueueEntry first = entries.get(0);

        respond(res, MusicUtils.formatNowPlaying(
                first.getTrack(), this.traqApi.getUserByID(first.getUserId()),
                state.getSetting(), true
        ));
    }

    /**
     * Handles "queue" command.
     * @param res Responder.
     * @param args Arguments.
     * @param state Music state.
     */
    public void handleQueue(Responder res, String[] args, MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        int page = 0;
        if (args.length > 2) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                respond(res, "Please input a valid number for pages!");
                return;
            }
        }

        respond(res, formatQueuePage(page, state));
    }

    private static final int SONGS_PER_PAGE = 10;

    private static int maxQueuePage(MusicState state) {
        int queueSize = state.getCurrentQueue().getQueue().size();
        // subtract one because the first one is the current song
        queueSize = Math.max(0, queueSize - 1);
        return (queueSize - 1) / SONGS_PER_PAGE;
    }

    private String formatQueuePage(int page, MusicState state) {
        RepeatState repeat = state.getSetting().getRepeat();
        int maxPage = maxQueuePage(state);

        List<String> ret = new ArrayList<>();
        ret.add(String.format("Current Queue%s", repeat.getMessage().isEmpty() ? "" : " : " + repeat.getMessage()));

        QueueState queueState = state.getCurrentQueue();
        List<QueueEntry> entries = queueState.getQueue();

        QueueEntry nowPlaying = entries.size() > 0 ? entries.get(0) : null;

        ret.add("__**Now Playing**__");
        if (nowPlaying != null) {
            ret.add(formatQueueEntry(nowPlaying, true));
        } else {
            ret.add("Nothing seems to be playing right now...");
        }

        int begin = page * SONGS_PER_PAGE + 1;
        int end = Math.min((page + 1) * SONGS_PER_PAGE + 1, entries.size());
        if (begin < end) {
            ret.add("");
            ret.add("__**Next Up**__");
            for (int i = begin; i < end; i++) {
                ret.add(String.format("**%s**. %s", i, formatQueueEntry(entries.get(i), false)));
            }
        }

        ret.add("");
        ret.add("");
        if (repeat.isEndlessMode()) {
            ret.add(String.format("`[%s]` track%s | Queue Length `[%s]`",
                    entries.size(), entries.size() == 1 ? "" : "s", formatLength(state.getQueueLength())));
        } else {
            ret.add(String.format("`[%s]` track%s | Remaining Length `[%s]`",
                    entries.size(), entries.size() == 1 ? "" : "s", formatLength(state.getRemainingLength())));
        }

        ret.add(String.format("Page [ %s / %s ]", page + 1, maxPage + 1));
        return String.join("\n", ret);
    }

    /**
     * Formats a single song in the queue page.
     * @param entry Queue entry.
     * @return Formatted entry.
     */
    private String formatQueueEntry(QueueEntry entry, boolean showPosition) {
        User user = this.traqApi.getUserByID(entry.getUserId());
        AudioTrack track = entry.getTrack();
        AudioTrackInfo info = track.getInfo();

        return String.format("[%s](%s) `[%s]` | Requested by %s",
                info.title, info.uri,
                showPosition ? formatLength(track.getPosition()) + "/" + formatLength(info.length) : formatLength(info.length),
                user != null ? user.getName() : "Unknown User");
    }

    /**
     * Handles "pause" and "resume" commands.
     * @param res Responder.
     * @param state Music state.
     * @param toStop If {@code true}, the bot should stop the player.
     */
    public void handlePause(Responder res, MusicState state, boolean toStop) {
        AudioPlayer player = state.getPlayer();
        AudioTrack np = player.getPlayingTrack();
        if (np == null) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        player.setPaused(toStop);

        if (toStop) {
            respond(res, "Paused the player.");
        } else {
            respond(res, "Resumed the player.");
        }
    }

    /**
     * Handles "skip" command.
     * @param res Responder.
     * @param args Command arguments.
     * @param state Music state.
     */
    public void handleSkip(Responder res, String[] args, MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        int skipAmount;
        if (args.length <= 2) {
            skipAmount = 1;
        } else {
            try {
                skipAmount = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                respond(res, "Please input a valid number for the skip amount!");
                return;
            }
            if (skipAmount < 1 || queueState.getQueue().size() < skipAmount) {
                respond(res, String.format("Please input a number between 1 and %s for the skip amount!",
                        queueState.getQueue().size()));
                return;
            }
        }

        respond(res, String.format("Skipping %s song%s.", skipAmount, skipAmount == 1 ? "" : "s"));
        state.skip(skipAmount);
    }

    /**
     * Handles "seek" command.
     * @param res Responder.
     * @param args Command arguments.
     * @param state Music state.
     */
    public void handleSeek(Responder res, String[] args, MusicState state) {
        if (args.length <= 2) {
            respond(res, "Input the time you want to seek to. e.g. `m seek 1:02:03`, `m seek 4:33`");
            return;
        }

        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        QueueEntry np = queueState.getQueue().get(0);
        if (!np.getTrack().isSeekable()) {
            respond(res, "This track is not seek-able.");
            return;
        }

        String positionStr = args[2];
        long position;
        try {
            position = parseLength(positionStr);
        } catch (IllegalArgumentException e) {
            respond(res, "Please input a valid time. e.g. `m seek 1:02:03`, `m seek 4:33`");
            return;
        }

        np.getTrack().setPosition(position);

        respond(res, String.format("Seeked to %s.", positionStr));
    }

    /**
     * Handles "shuffle" command.
     * @param res Responder.
     * @param state Music state.
     */
    public void handleShuffle(Responder res, MusicState state) {
        QueueState queueState = state.getCurrentQueue();
        if (queueState.getQueue().isEmpty()) {
            respond(res, "Nothing seems to be playing right now.");
            return;
        }

        state.shuffle();
        respond(res, "Shuffled the queue.");
    }

    /**
     * Handles "purge" command.
     * @param res Responder.
     * @param state Music state.
     */
    public void handlePurge(Responder res, MusicState state) {
        state.stopLoadingCache();
        state.purgeWaitingQueue();
        respond(res, "Purged the queue.");
    }
}
