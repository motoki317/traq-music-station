package app;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

public interface EventListener {
    void onMessageReceived(@NotNull MessageCreatedEvent event, @NotNull Responder res);
}
