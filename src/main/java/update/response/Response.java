package update.response;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import update.base.UserResponseListener;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// Represents bot's response to user's response (input).
public class Response implements UserResponseListener<MessageCreatedEvent> {
    private final UUID channelId;
    private final UUID userId;

    private final Predicate<MessageCreatedEvent> onResponse;
    // Called when this instance is discarded by manager
    private Runnable onDestroy;

    private final long updatedAt;
    private final long maxLive;

    public Response(UUID channelId,
                    UUID userId,
                    Predicate<MessageCreatedEvent> onResponse) {
        this.channelId = channelId;
        this.userId = userId;
        this.onResponse = onResponse;
        this.onDestroy = () -> {};
        this.updatedAt = System.currentTimeMillis();
        this.maxLive = TimeUnit.MINUTES.toMillis(10);
    }

    // boolean returned by predicate indicates if manager should discard this response object.
    public boolean handle(MessageCreatedEvent event) {
        return this.onResponse.test(event);
    }

    UUID getChannelId() {
        return channelId;
    }

    UUID getUserId() {
        return userId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getMaxLive() {
        return maxLive;
    }

    public void onDestroy() {
        this.onDestroy.run();
    }

    public void setOnDestroy(Runnable onDestroy) {
        this.onDestroy = onDestroy;
    }
}
