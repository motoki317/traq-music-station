package music;

import api.TraqApi;
import com.github.motoki317.traq4j.model.User;
import com.github.motoki317.traq4j.model.WebRTCUserState;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import db.model.musicSetting.MusicSetting;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MusicUtils {
    /**
     * Formats time length.
     * @param milliseconds Length in milliseconds.
     * @return Formatted string such as "4:33".
     */
    public static String formatLength(long milliseconds) {
        long seconds = milliseconds / 1000L;

        long hours = seconds / 3600L;
        seconds -= hours * 3600L;
        long minutes = seconds / 60L;
        seconds -= minutes * 60L;

        if (hours > 0L) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    private final static Pattern lengthHourPattern = Pattern.compile("(\\d+):(\\d{2}):(\\d{2})");
    private final static Pattern lengthMinutePattern = Pattern.compile("(\\d{1,2}):(\\d{2})");

    /**
     * Parses time length.
     * @param length Length string, such as "1:23:45" or "5:34".
     * @return Length in milliseconds.
     * @throws IllegalArgumentException If the given length is not a valid pattern.
     */
    public static long parseLength(String length) throws IllegalArgumentException {
        Matcher hour = lengthHourPattern.matcher(length);
        if (hour.matches()) {
            long hours = Integer.parseInt(hour.group(1));
            long minutes = Integer.parseInt(hour.group(2));
            long seconds = Integer.parseInt(hour.group(3));
            return TimeUnit.HOURS.toMillis(hours) +
                    TimeUnit.MINUTES.toMillis(minutes) +
                    TimeUnit.SECONDS.toMillis(seconds);
        }
        Matcher minute = lengthMinutePattern.matcher(length);
        if (minute.matches()) {
            long minutes = Integer.parseInt(minute.group(1));
            long seconds = Integer.parseInt(minute.group(2));
            return TimeUnit.MINUTES.toMillis(minutes) +
                    TimeUnit.SECONDS.toMillis(seconds);
        }
        throw new IllegalArgumentException("Failed to parse length");
    }

    /**
     * Retrieves voice channel the user is in.
     * @param traqApi traQ API
     * @param userId User UUID
     * @return Qall state. null if not found.
     */
    @Nullable
    public static QallState getVoiceChannel(@NotNull TraqApi traqApi, @NotNull UUID userId) {
        List<WebRTCUserState> states = traqApi.getWebRTCState();
        if (states == null) {
            return null;
        }
        for (WebRTCUserState state : states) {
            if (state.getUserId().equals(userId)) {
                var sessions = state.getSessions();
                QallState qs;
                if (sessions.size() == 0) {
                    qs = new QallState(state.getChannelId(), "");
                } else {
                    qs = new QallState(state.getChannelId(), sessions.get(0).getSessionId());
                }
                return qs;
            }
        }
        return null;
    }

    /**
     * Retrieves voice channel by channel ID.
     * @param traqApi traQ API
     * @param channelId Channel ID
     * @return Qall state. null if not found.
     */
    @Nullable
    public static QallState getVoiceChannelByID(@NotNull TraqApi traqApi, @NotNull UUID channelId) {
        List<WebRTCUserState> states = traqApi.getWebRTCState();
        if (states == null) {
            return null;
        }
        for (WebRTCUserState state : states) {
            if (state.getChannelId().equals(channelId)) {
                var sessions = state.getSessions();
                QallState qs;
                if (sessions.size() == 0) {
                    qs = new QallState(state.getChannelId(), "");
                } else {
                    qs = new QallState(state.getChannelId(), sessions.get(0).getSessionId());
                }
                return qs;
            }
        }
        return null;
    }

    /**
     * Formats "now playing" message.
     * @param track Audio track.
     * @param user User who requested this track.
     * @param setting Music setting.
     * @param showPosition If {@code true}, displays current position.
     * @return Formatted message
     */
    @NotNull
    public static String formatNowPlaying(AudioTrack track, @Nullable User user,
                                          MusicSetting setting, boolean showPosition) {
        AudioTrackInfo info = track.getInfo();
        String title = info.title;
        RepeatState repeat = setting.getRepeat();

        String length = info.isStream ? "LIVE" : formatLength(info.length);
        String lengthField = showPosition
                ? String.format("%s / %s", formatLength(track.getPosition()), length)
                : length;

        return String.format("♪ Now playing%s\n" +
                        "[%s](%s)\n" +
                        "Length: %s\n" +
                        "Player volume: %s\n" +
                        "Requested by %s",
                repeat == RepeatState.OFF ? "" : " (" + repeat.getMessage() + ")",
                "".equals(title) ? "(No title)" : title, track.getInfo().uri,
                lengthField,
                setting.getVolume() + "%",
                user == null ? "unknown user" : user.getName());
    }
}
