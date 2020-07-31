package log;

import com.github.motoki317.traq_bot.model.MessageCreatedEvent;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SpamChecker {
    // 1 second
    private static final long SPAM_PREVENTION = TimeUnit.SECONDS.toMillis(1);

    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(1);

    // User ID to last message time (epoch milliseconds)
    private final Map<UUID, Long> messages;

    public SpamChecker() {
        this.messages = new HashMap<>();
    }

    public synchronized boolean isSpam(MessageCreatedEvent event) {
        UUID userId = UUID.fromString(event.getMessage().getUser().getId());
        long time = event.getBasePayload().getEventTime().atZone(ZoneOffset.UTC).toEpochSecond() * 1000;
        long lastMessageTime = this.messages.getOrDefault(userId, -1L);

        if (lastMessageTime == -1L) {
            this.messages.put(userId, time);
            return false;
        }

        long diff = time - lastMessageTime;
        if (diff < 0) {
            return false;
        }
        if (diff < SPAM_PREVENTION) {
            return true;
        }
        this.messages.put(userId, time);

        this.removeOldMessageCache(time);
        return false;
    }

    private void removeOldMessageCache(long currentTime) {
        this.messages.entrySet().removeIf((e) -> CACHE_TIME < (currentTime - e.getValue()));
    }
}
