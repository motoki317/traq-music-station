import app.AppImpl;
import app.Properties;
import update.UpdaterFactoryImpl;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        AppImpl appImpl = new AppImpl(new Properties(), new UpdaterFactoryImpl());
        Thread appThread = new Thread(appImpl);
        appThread.setName("bot app");
        appThread.start();

        Runtime.getRuntime().addShutdownHook(new Thread(appImpl::onShutDown));
    }
}
