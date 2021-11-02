package app;

import api.TraqApi;
import api.TraqApiImpl;
import com.github.motoki317.traq4j.api.MessageApi;
import com.github.motoki317.traq4j.model.PostMessageRequest;
import com.github.motoki317.traq_ws_bot.Bot;
import com.github.motoki317.traq_ws_bot.Event;
import com.github.motoki317.traq_ws_bot.WebRTCState;
import com.github.motoki317.traq_ws_bot.model.DirectMessageCreatedEvent;
import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
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
import java.util.UUID;

public class AppImpl implements Runnable, App {
    private final TraqApi traqApi;

    private final SkywayApi skywayApi;

    private final Properties properties;

    private final Database database;

    private final Logger logger;

    private final ResponseManager responseManager;

    private final List<EventListener> listeners;

    private final Bot bot;

    private final MusicServer musicServer;

    private final MessageApi messageApi;

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

    @Override
    public void sendWebRTCState(String channelId, WebRTCState... states) {
        this.bot.sendWebRTCState(channelId, states);
    }

    public AppImpl(Properties properties, UpdaterFactory updaterFactory) throws IOException {
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

        this.bot = new Bot(
                System.getenv("ACCESS_TOKEN"),
                System.getenv("TRAQ_API_BASE_PATH"),
                true
        );
        bot.onEvent(Event.MESSAGE_CREATED, MessageCreatedEvent.class, (e) -> {
            Responder r = newResponder(e.message().channelId());
            for (EventListener listener : AppImpl.this.listeners) {
                listener.onMessageReceived(e, r);
            }
        });
        bot.onEvent(Event.DIRECT_MESSAGE_CREATED, DirectMessageCreatedEvent.class, (e) -> {
            Responder r = newResponder(e.message().channelId());
            for (EventListener listener : AppImpl.this.listeners) {
                listener.onMessageReceived(new MessageCreatedEvent(
                        e.basePayload().eventTime().toString(),
                        e.message()
                ), r);
            }
        });
        bot.onEvent(Event.ERROR, String.class, (m) ->
                System.out.printf("Received ERROR event from traQ: %s\n", m));

        this.messageApi = new MessageApi(this.bot.getClient());

        this.logger.log("Bot load complete!");
    }

    public void run() {
        this.bot.start();
    }

    private void addEventListener(EventListener listener) {
        this.listeners.add(listener);
    }

    public void onShutDown() {
        this.logger.log("Bot shutting down...");
    }

    private Responder newResponder(String channelId) {
        return (content, embed) -> this.messageApi.postMessage(
                UUID.fromString(channelId),
                new PostMessageRequest().content(content).embed(embed)
        );
    }
}
