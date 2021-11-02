package app;

import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class Properties {
    private final java.util.Properties properties;

    public final String version;

    public final Date lastReboot;

    public final String botUserId;

    public final Map<Integer, Long> logChannelId;

    public final String prefix;

    private final String mainColor;

    final TimeZone logTimeZone;

    public Properties() throws IOException {
        this.properties = new java.util.Properties();
        this.properties.load(this.getClass().getClassLoader().getResourceAsStream("project.properties"));

        this.version = getProperty("version");

        this.lastReboot = new Date();

        this.botUserId = getEnv("BOT_USER_ID");

        this.logChannelId = new HashMap<>();

        this.prefix = getProperty("prefix");

        this.mainColor = getProperty("mainColor");

        this.logTimeZone = TimeZone.getTimeZone(getProperty("logTimeZone"));
    }

    private String getEnv(String name) {
        return System.getenv(name);
    }

    private String getProperty(String name) {
        return this.properties.getProperty(name);
    }

    /**
     * Converts hex color string "#FFFFFF" to java color instance.
     * @return Color instance.
     */
    public Color getMainColor() {
        return new Color(
                Integer.valueOf( this.mainColor.substring( 1, 3 ), 16 ),
                Integer.valueOf( this.mainColor.substring( 3, 5 ), 16 ),
                Integer.valueOf( this.mainColor.substring( 5, 7 ), 16 )
        );
    }
}
