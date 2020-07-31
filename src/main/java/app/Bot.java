package app;

import api.TraqApi;
import db.Database;
import http.MusicServer;
import log.Logger;
import skyway.SkywayApi;
import update.response.ResponseManager;

public interface Bot {
    Database getDatabase();
    Properties getProperties();
    MusicServer getMusicServer();
    TraqApi getTraqApi();
    SkywayApi getSkywayApi();
    Logger getLogger();
    ResponseManager getResponseManager();
}
