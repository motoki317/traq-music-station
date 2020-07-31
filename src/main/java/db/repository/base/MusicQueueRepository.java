package db.repository.base;

import db.ConnectionPool;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicQueue.MusicQueueEntryId;
import log.Logger;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.List;

public abstract class MusicQueueRepository extends Repository<MusicQueueEntry, MusicQueueEntryId> {
    protected MusicQueueRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Deletes all entries of the channel.
     * @param channelId Channel ID.
     * @return {@code true} if success.
     */
    public abstract boolean deleteGuildMusicQueue(String channelId);

    /**
     * Saves all given entries.
     * @param queue Music queue.
     * @return {@code true} if success.
     */
    public abstract boolean saveGuildMusicQueue(List<MusicQueueEntry> queue);

    /**
     * Retrieves channel's music queue, ordered by their index.
     * @param channelId Channel ID.
     * @return Music queue.
     */
    @Nullable
    public abstract List<MusicQueueEntry> getGuildMusicQueue(String channelId);

    /**
     * Deletes all entries older than the specified date.
     * @param threshold Deletes if older than this date.
     * @return {@code true} if success.
     */
    public abstract boolean deleteAllOlderThan(Date threshold);
}
