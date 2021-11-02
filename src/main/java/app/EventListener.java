package app;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

public interface EventListener {
    void onMessageReceived(@NotNull MessageCreatedEvent event, @NotNull Responder res);
}
