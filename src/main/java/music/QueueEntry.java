package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.UUID;

/**
 * Queue entry record
 */
public class QueueEntry {
    private final AudioTrack track;
    private final UUID userId;

    public QueueEntry(AudioTrack track, UUID userId) {
        this.track = track;
        this.userId = userId;
    }

    public AudioTrack getTrack() {
        return track;
    }

    public UUID getUserId() {
        return userId;
    }
}
