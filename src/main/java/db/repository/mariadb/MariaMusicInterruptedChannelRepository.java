package db.repository.mariadb;

import db.ConnectionPool;
import db.model.musicInterruptedChannel.MusicInterruptedChannel;
import db.model.musicInterruptedChannel.MusicInterruptedChannelId;
import db.repository.base.MusicInterruptedChannelRepository;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

class MariaMusicInterruptedChannelRepository extends MusicInterruptedChannelRepository {
    MariaMusicInterruptedChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    @Override
    protected MusicInterruptedChannel bind(@NotNull ResultSet res) throws SQLException {
        return new MusicInterruptedChannel(res.getString(1), res.getString(2));
    }

    @Override
    public <S extends MusicInterruptedChannel> boolean create(@NotNull S entity) {
        return this.execute(
                "INSERT INTO `music_interrupted_channel` (vc_id, text_channel_id) VALUES (?, ?)",
                entity.getVcId(),
                entity.getTextChannelId()
        );
    }

    @Override
    public boolean createAll(List<MusicInterruptedChannel> channels) {
        if (channels.isEmpty()) {
            return true;
        }
        return this.execute(
                "INSERT INTO `music_interrupted_channel` (vc_id, text_channel_id) VALUES " +
                        String.join(", ", Collections.nCopies(channels.size(), "(?, ?)")),
                channels.stream().flatMap(ch -> Stream.of(
                        ch.getVcId(),
                        ch.getTextChannelId()
                )).toArray()
        );
    }

    @Override
    public boolean deleteAll() {
        return this.execute(
                "TRUNCATE TABLE `music_interrupted_channel`"
        );
    }

    @Override
    public boolean exists(@NotNull MusicInterruptedChannelId musicInterruptedChannelId) {
        ResultSet res = this.executeQuery(
                "SELECT COUNT(*) FROM `music_interrupted_channel` WHERE vc_id = ?",
                musicInterruptedChannelId.getVcId()
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
                "SELECT COUNT(*) FROM `music_interrupted_channel`"
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
    public MusicInterruptedChannel findOne(@NotNull MusicInterruptedChannelId musicInterruptedChannelId) {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_interrupted_channel` WHERE vc_id = ?",
                musicInterruptedChannelId.getVcId()
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
    public List<MusicInterruptedChannel> findAll() {
        ResultSet res = this.executeQuery(
                "SELECT * FROM `music_interrupted_channel`"
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
    public boolean update(@NotNull MusicInterruptedChannel entity) {
        return this.execute(
                "UPDATE `music_interrupted_channel` SET `text_channel_id` = ? WHERE `vc_id` = ?",
                entity.getTextChannelId(),
                entity.getVcId()
        );
    }

    @Override
    public boolean delete(@NotNull MusicInterruptedChannelId musicInterruptedChannelId) {
        return this.execute(
                "DELETE FROM `music_interrupted_channel` WHERE `vc_id` = ?",
                musicInterruptedChannelId.getVcId()
        );
    }
}
