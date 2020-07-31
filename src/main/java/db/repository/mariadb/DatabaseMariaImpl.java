package db.repository.mariadb;

import db.ConnectionPool;
import db.Database;
import db.SimpleConnectionPool;
import db.repository.base.*;
import log.Logger;
import org.jetbrains.annotations.NotNull;

public class DatabaseMariaImpl implements Database {
    private static final String MYSQL_HOST = System.getenv("MYSQL_HOST");
    private static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    private static final String MYSQL_USER = System.getenv("MYSQL_USER");
    private static final String MYSQL_PASSWORD = System.getenv("MYSQL_PASSWORD");
    private static final int MYSQL_PORT = Integer.parseInt(System.getenv("MYSQL_PORT"));

    private static final String URL =  String.format(
            "jdbc:mariadb://%s:%s/%s?user=%s&password=%s",
            MYSQL_HOST, MYSQL_PORT, MYSQL_DATABASE, MYSQL_USER, MYSQL_PASSWORD);

    private final Logger logger;

    private final ConnectionPool connectionPool;

    // Repository instance cache
    private CommandLogRepository commandLogRepository;
    private MusicSettingRepository musicSettingRepository;
    private MusicQueueRepository musicQueueRepository;
    private MusicInterruptedChannelRepository musicInterruptedChannelRepository;

    public DatabaseMariaImpl(Logger logger) {
        this.logger = logger;
        this.connectionPool = new SimpleConnectionPool(URL, logger, 10);
    }

    @NotNull
    @Override
    public CommandLogRepository getCommandLogRepository() {
        if (this.commandLogRepository == null) {
            this.commandLogRepository = new MariaCommandLogRepository(this.connectionPool, this.logger);
        }
        return this.commandLogRepository;
    }

    @Override
    public @NotNull MusicSettingRepository getMusicSettingRepository() {
        if (this.musicSettingRepository == null) {
            this.musicSettingRepository = new MariaMusicSettingRepository(this.connectionPool, this.logger);
        }
        return this.musicSettingRepository;
    }

    @Override
    public @NotNull MusicQueueRepository getMusicQueueRepository() {
        if (this.musicQueueRepository == null) {
            this.musicQueueRepository = new MariaMusicQueueRepository(this.connectionPool, this.logger);
        }
        return this.musicQueueRepository;
    }

    @Override
    public @NotNull MusicInterruptedChannelRepository getMusicInterruptedChannelRepository() {
        if (this.musicInterruptedChannelRepository == null) {
            this.musicInterruptedChannelRepository = new MariaMusicInterruptedChannelRepository(this.connectionPool, this.logger);
        }
        return this.musicInterruptedChannelRepository;
    }
}
