package app;

import api.TraqApi;
import api.TraqApiImpl;
import com.github.motoki317.traq_bot.BotServer;
import com.github.motoki317.traq_bot.EventHandlers;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import db.Database;
import db.repository.mariadb.DatabaseMariaImpl;
import http.MusicServer;
import http.MusicServerImpl;
import log.ConsoleLogger;
import log.Logger;
import skyway.SkywayApi;
import skyway.SkywayApiImpl;
import update.UpdaterFactory;
import update.response.ResponseManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class App implements Runnable, Bot {
    private final TraqApi traqApi;

    private final SkywayApi skywayApi;

    private final Properties properties;

    private final Database database;

    private final Logger logger;

    private final ResponseManager responseManager;

    private final List<EventListener> listeners;

    private final BotServer server;

    private final MusicServer musicServer;

    @Override
    public MusicServer getMusicServer() {
        return musicServer;
    }

    @Override
    public TraqApi getTraqApi() {
        return this.traqApi;
    }

    @Override
    public SkywayApi getSkywayApi() {
        return skywayApi;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public Database getDatabase() {
        return this.database;
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public ResponseManager getResponseManager() {
        return this.responseManager;
    }

    public App(Properties properties, UpdaterFactory updaterFactory) throws IOException {
        this.properties = properties;
        this.logger = new ConsoleLogger(this.properties.logTimeZone);
        this.traqApi = new TraqApiImpl(
                System.getenv("TRAQ_API_BASE_PATH"),
                System.getenv("ACCESS_TOKEN")
        );
        this.skywayApi = new SkywayApiImpl(this.logger);
        this.responseManager = updaterFactory.getResponseManager();
        this.listeners = new ArrayList<>();
        this.musicServer = new MusicServerImpl(Integer.parseInt(System.getenv("MUSIC_PORT")), this);

        this.database = new DatabaseMariaImpl(this.logger);

        this.addEventListener(new CommandListener(this));
        this.addEventListener(new UpdaterListener(this));
        this.logger.debug("Added event listeners.");

        EventHandlers handlers = new EventHandlers();
        handlers.setMessageCreatedHandler((e, r) -> {
            for (EventListener listener : App.this.listeners) {
                listener.onMessageReceived(e, r);
            }
        });
        handlers.setDirectMessageCreatedHandler((e, r) -> {
            for (EventListener listener : App.this.listeners) {
                listener.onMessageReceived(new MessageCreatedEvent(
                        e.getBasePayload().getEventTime().toString(),
                        e.getMessage()
                ), r);
            }
        });
        this.server = new BotServer(
                System.getenv("VERIFICATION_TOKEN"),
                System.getenv("ACCESS_TOKEN"),
                System.getenv("TRAQ_API_BASE_PATH"),
                Integer.parseInt(System.getenv("BOT_PORT")),
                handlers
        );

        this.logger.log("Bot load complete!");
    }

    public void run() {
        this.server.start();
    }

    private void addEventListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void onShutDown() {
        this.logger.log("Bot shutting down...");
    }
}
