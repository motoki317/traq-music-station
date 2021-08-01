package skyway;

import log.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogEntries;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.WebDriverWait;
import utils.StoppableThread;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class SkywayApiImpl implements SkywayApi {
    private static class Player {
        private final ChromeDriver driver;
        private final StoppableThread player;
        private final StoppableThread log;

        public Player(ChromeDriver driver, StoppableThread player, StoppableThread log) {
            this.driver = driver;
            this.player = player;
            this.log = log;
        }
    }

    private final Map<UUID, Player> players;
    private final Logger logger;

    public SkywayApiImpl(Logger logger) {
        this.players = new HashMap<>();
        this.logger = logger;
    }

    @Override
    public synchronized void joinChannel(String audioOrigin, UUID channelId) {
        this.logger.log("[Selenium] Joining channel " + channelId.toString() + "...");
        ChromeOptions options = new ChromeOptions()
                .setBinary("/usr/bin/chromium")
                .addArguments("--no-sandbox")
                .addArguments("--disable-dev-shm-usage")
                .setHeadless(true);
        ChromeDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 60);
        driver.setLogLevel(Level.ALL);
        driver.get(audioOrigin);

        StoppableThread player = new StoppableThread() {
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
            }

            @Override
            protected void cleanUp() {}
        };
        StoppableThread log = new StoppableThread() {
            @Override
            public void run() {
                Logger logger = SkywayApiImpl.this.logger;
                LogEntries logs = driver.manage().logs().get(LogType.BROWSER);
                for (var log : logs) {
                    logger.log("[Selenium console] " + log.toString());
                }
            }

            @Override
            protected void cleanUp() {}
        };

        player.start();
        log.start();
        this.players.put(channelId, new Player(
                driver,
                player,
                log
        ));
    }

    @Override
    public synchronized void close(UUID channelID) {
        if (!this.players.containsKey(channelID)) return;

        this.logger.log("[Selenium] Closing channel " + channelID.toString() + "...");
        Player player = this.players.get(channelID);
        this.players.remove(channelID);

        // Press disconnect button via chrome driver
        var driver = player.driver;
        WebDriverWait wait = new WebDriverWait(driver, 60);
        var disconnectButton = wait.until(presenceOfElementLocated(By.id("button-disconnect")));
        disconnectButton.click();
        driver.close();

        // Terminate player (if not finished) and log loop
        if (player.player.isAlive()) {
            player.player.terminate();
        }
        if (player.log.isAlive()) {
            player.log.terminate();
        }
    }
}
