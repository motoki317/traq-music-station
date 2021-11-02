package log;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;

public interface Logger {
    void log(CharSequence message);
    void debug(CharSequence message);

    /**
     * Logs message received event.
     * @param event Message received event.
     * @param isSpam If the message was considered spam.
     */
    void logEvent(MessageCreatedEvent event, boolean isSpam);
    void logException(CharSequence message, Throwable e);
}
