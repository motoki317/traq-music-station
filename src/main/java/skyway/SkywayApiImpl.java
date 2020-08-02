package skyway;

import log.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.StoppableThread;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class SkywayApiImpl implements SkywayApi {
    private final Map<UUID, ChromeDriver> drivers;
    private final Map<UUID, StoppableThread> logs;
    private final Logger logger;

    public SkywayApiImpl(Logger logger) {
        this.drivers = new HashMap<>();
        this.logs = new HashMap<>();
        this.logger = logger;
    }

    @Override
    public synchronized void joinChannel(String audioOrigin, UUID channelId) {
        this.logger.log("[Selenium] Joining channel " + channelId.toString() + "...");
        ChromeOptions options = new ChromeOptions()
                .setBinary("/usr/bin/google-chrome")
                .addArguments("--no-sandbox")
                .addArguments("--disable-dev-shm-usage")
                .setHeadless(true);
        ChromeDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.setLogLevel(Level.ALL);
        driver.get(audioOrigin);

        StoppableThread log = new StoppableThread() {
            @Override
            public void run() {
                Logger logger = SkywayApiImpl.this.logger;
                // Interact with the document first before playing
                var title = wait.until(presenceOfElementLocated(By.id("title")));
                title.click();

                WebDriverWait waitConnected = new WebDriverWait(driver, Integer.MAX_VALUE, 250);
                // Wait until the Skyway API connected to the room
                waitConnected.until(presenceOfElementLocated(By.id("connected-flag")));
                var playButton = wait.until(presenceOfElementLocated(By.id("button-play")));
                logger.log("[Selenium] Playing...");
                playButton.click();

                LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
                for (Iterator<LogEntry> iterator = logs.iterator(); iterator.hasNext() && this.isActive; ) {
                    LogEntry log = iterator.next();
                    SkywayApiImpl.this.logger.log("[Selenium console] " + log.toString());
                }
            }

            @Override
            protected void cleanUp() {}
        };
        log.start();
        this.logs.put(channelId, log);
        this.drivers.put(channelId, driver);
    }

    @Override
    public synchronized void close(UUID channelID) {
        if (!this.drivers.containsKey(channelID) && !this.logs.containsKey(channelID)) return;

        this.logger.log("[Selenium] Closing channel " + channelID.toString() + "...");

        if (this.drivers.containsKey(channelID)) {
            var driver = this.drivers.get(channelID);
            WebDriverWait wait = new WebDriverWait(driver, 60);
            var disconnectButton = wait.until(presenceOfElementLocated(By.id("button-disconnect")));
            disconnectButton.click();
            driver.close();
            this.drivers.remove(channelID);
        }
        if (this.logs.containsKey(channelID)) {
            this.logs.get(channelID).terminate();
            this.logs.remove(channelID);
        }
    }
}
