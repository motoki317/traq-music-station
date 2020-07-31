package app;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;
import update.response.ResponseManager;

public class UpdaterListener implements EventListener {
    private final ResponseManager responseManager;

    UpdaterListener(Bot bot) {
        this.responseManager = bot.getResponseManager();
    }

    @Override
    public void onMessageReceived(@NotNull MessageCreatedEvent event, @NotNull Responder res) {
        this.responseManager.handle(event);
    }
}
