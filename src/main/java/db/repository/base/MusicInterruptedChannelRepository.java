package db.repository.base;

import db.ConnectionPool;
import db.model.musicInterruptedChannel.MusicInterruptedChannel;
import db.model.musicInterruptedChannel.MusicInterruptedChannelId;
import log.Logger;

import java.util.List;

public abstract class MusicInterruptedChannelRepository extends Repository<MusicInterruptedChannel, MusicInterruptedChannelId> {
    protected MusicInterruptedChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Create all entries.
     * @param channels Entries.
     * @return {@code true} if success.
     */
    public abstract boolean createAll(List<MusicInterruptedChannel> channels);

    /**
     * Deletes all entries.
     * @return {@code true} if success.
     */
    public abstract boolean deleteAll();
}
