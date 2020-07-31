package update;

import update.response.ResponseManager;
import update.response.ResponseManagerImpl;

public class UpdaterFactoryImpl implements UpdaterFactory {
    @Override
    public ResponseManager getResponseManager() {
        return new ResponseManagerImpl();
    }
}
