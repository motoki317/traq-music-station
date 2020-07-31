package db.repository.mariadb;

import db.ConnectionPool;
import db.model.musicQueue.MusicQueueEntry;
import db.model.musicQueue.MusicQueueEntryId;
import db.repository.base.MusicQueueRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

class MariaMusicQueueRepository extends MusicQueueRepository {
    private static final DateFormat dbFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    MariaMusicQueueRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected MusicQueueEntry bind(@NotNull ResultSet res) throws SQLException {
        return new MusicQueueEntry(
                res.getString(1),
                res.getInt(2),
                UUID.fromString(res.getString(3)),
                res.getString(4),
                res.getInt(5),
                res.getTimestamp(6)
        );
    }

    @Override
    public <S extends MusicQueueEntry> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `music_queue` (channel_id, `index`, user_id, url, position, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                entity.getChannelId(),
                entity.getIndex(),
                entity.getUserId().toString(),
                entity.getUrl(),
                entity.getPosition(),
                dbFormat.format(entity.getUpdatedAt())
        );
    }

    @Override
    public boolean exists(@NotNull MusicQueueEntryId musicQueueEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `music_queue` WHERE `channel_id` = ? AND `index` = ?",
                musicQueueEntryId.getChannelId(),
                musicQueueEntryId.getIndex()
        );

        if (res == null) {
            return false;
        }

        try {
            if (res.next())
                return res.getInt(1) > 0;
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return false;
    }

    @Override
    public long count() {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `music_queue`"
        );

        if (res == null) {
            return -1;
        }

        try {
            if (res.next())
                return res.getInt(1);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return -1;
    }

    @Nullable
    @Override
    public MusicQueueEntry findOne(@NotNull MusicQueueEntryId musicQueueEntryId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_queue` WHERE `channel_id` = ? AND `index` = ?",
                musicQueueEntryId.getChannelId(),
                musicQueueEntryId.getIndex()
        );

        if (res == null) {
            return null;
        }

        try {
            if (res.next())
                return bind(res);
        } catch (SQLException e) {
            this.logResponseException(e);
        }
        return null;
    }

    @Nullable
    @Override
    public List<MusicQueueEntry> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_queue`"
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public boolean deleteGuildMusicQueue(String channelId) {
        return this.execute(
                "DELETE FROM `music_queue` WHERE `channel_id` = ?",
                channelId
        );
    }

    @Override
    public boolean saveGuildMusicQueue(List<MusicQueueEntry> queue) {
        if (queue.isEmpty()) {
            return true;
        }

        String placeHolder = "(?, ?, ?, ?, ?, ?)";
        return this.execute(
                "INSERT INTO `music_queue` (channel_id, `index`, user_id, url, position, updated_at) VALUES " +
                        String.join(", ", Collections.nCopies(queue.size(), placeHolder)),
                queue.stream().flatMap(q -> Stream.of(
                        q.getChannelId(),
                        q.getIndex(),
                        q.getUserId().toString(),
                        q.getUrl(),
                        q.getPosition(),
                        dbFormat.format(q.getUpdatedAt())
                )).toArray()
        );
    }

    @Nullable
    @Override
    public List<MusicQueueEntry> getGuildMusicQueue(String channelId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_queue` WHERE `channel_id` = ? ORDER BY `index`",
                channelId
        );

        if (res == null) {
            return null;
        }

        try {
            return bindAll(res);
        } catch (SQLException e) {
            this.logResponseException(e);
            return null;
        }
    }

    @Override
    public boolean deleteAllOlderThan(Date threshold) {
        return this.execute(
                "DELETE FROM `music_queue` WHERE `updated_at` < ?",
                dbFormat.format(threshold)
        );
    }

    @Override
    public boolean update(@NotNull MusicQueueEntry entity) {
        return this.execute(
                "UPDATE `music_queue` SET `user_id` = ?, `url` = ?, `position` = ?, `updated_at` = ? WHERE `channel_id` = ? AND `index` = ?",
                entity.getUserId().toString(),
                entity.getUrl(),
                entity.getPosition(),
                dbFormat.format(entity.getUpdatedAt()),
                entity.getChannelId(),
                entity.getIndex()
        );
    }

    @Override
    public boolean delete(@NotNull MusicQueueEntryId musicQueueEntryId) {
        return this.execute(
                "DELETE FROM `music_queue` WHERE `channel_id` = ? AND `index` = ?",
                musicQueueEntryId.getChannelId(),
                musicQueueEntryId.getIndex()
        );
    }
}
