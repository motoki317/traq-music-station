package log;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * ConsoleLogger only logs to standard output.
 */
public class ConsoleLogger implements Logger {
    private final DateFormat logFormat;
    private final boolean debug;

    public ConsoleLogger(TimeZone logTimeZone) {
        this.logFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");
        this.logFormat.setTimeZone(logTimeZone);
        this.debug = "1".equals(System.getenv("DEBUG"));
    }

    @Override
    public void log(CharSequence message) {
        Date now = new Date();
        String msg = this.logFormat.format(now) + " " + message;
        System.out.println(msg);
    }

    @Override
    public void debug(CharSequence message) {
        if (!debug) return;

        this.log(message);
    }

    @Override
    public void logEvent(MessageCreatedEvent event, boolean isSpam) {
        String logMsg = createCommandLog(event, isSpam);
        this.log(logMsg);
    }

    /**
     * Create a user command log String. <br/>
     * Example output <br/>
     * 2018/10/01 11:02:46.430 [Channel]&lt;User&gt;: `>ping` <br/>
     *
     * @param event Guild message received event.
     * @return Human readable user command usage log.
     */
    static String createCommandLog(MessageCreatedEvent event, boolean isSpam) {
        return String.format(
                "[%s]<%s>: `%s`%s",
                event.message().channelId(),
                event.message().user().name(),
                event.message().plainText(),
                isSpam ? " Spam detected" : ""
        );
    }

    @Override
    public void logException(CharSequence message, Throwable e) {
        this.log(message);
        e.printStackTrace();
    }
}
