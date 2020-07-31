package db.model.musicQueue;

import java.util.Date;
import java.util.UUID;

public class MusicQueueEntry implements MusicQueueEntryId {
    private final String channelId;
    private final int index;
    private final UUID userId;
    private final String url;
    private final long position;
    private final Date updatedAt;

    public MusicQueueEntry(String channelId, int index, UUID userId, String url, long position, Date updatedAt) {
        this.channelId = channelId;
        this.index = index;
        this.userId = userId;
        this.url = url;
        this.position = position;
        this.updatedAt = updatedAt;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public int getIndex() {
        return index;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUrl() {
        return url;
    }

    public long getPosition() {
        return position;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}
