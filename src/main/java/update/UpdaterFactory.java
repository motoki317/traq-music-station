package update;

import update.response.ResponseManager;

// Abstract factory
public interface UpdaterFactory {
    ResponseManager getResponseManager();
}
