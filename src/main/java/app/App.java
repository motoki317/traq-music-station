package app;

import api.TraqApi;
import com.github.motoki317.traq_ws_bot.WebRTCState;
import db.Database;
import http.MusicServer;
import log.Logger;
import skyway.SkywayApi;
import update.response.ResponseManager;

public interface App {
    Database getDatabase();
    Properties getProperties();
    MusicServer getMusicServer();
    TraqApi getTraqApi();
    SkywayApi getSkywayApi();
    Logger getLogger();
    ResponseManager getResponseManager();
    void sendWebRTCState(String channelId, WebRTCState ...states);
}
