package skyway;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import static org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated;

public class SkywayApiImpl implements SkywayApi {
    private final Map<UUID, ChromeDriver> drivers;

    public SkywayApiImpl() {
        this.drivers = new HashMap<>();
    }

    @Override
    public synchronized void joinChannel(String audioOrigin, UUID channelId) {
        System.out.println("[Selenium] Joining channel " + channelId.toString() + ", url " + audioOrigin);
        ChromeOptions options = new ChromeOptions()
                .setBinary("/usr/bin/google-chrome")
                .addArguments("--no-sandbox")
                .addArguments("--disable-dev-shm-usage")
                .setHeadless(true);
        ChromeDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, 10);
        driver.setLogLevel(Level.ALL);
        driver.get(audioOrigin);
        WebElement elt = wait.until(presenceOfElementLocated(By.id("title")));
        elt.click();

        this.drivers.put(channelId, driver);
    }

    @Override
    public synchronized void close(UUID channelID) {
        if (!this.drivers.containsKey(channelID)) return;

        this.drivers.get(channelID).close();
    }
}
