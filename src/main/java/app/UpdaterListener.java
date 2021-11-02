package app;

import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;
import update.response.ResponseManager;

public class UpdaterListener implements EventListener {
    private final ResponseManager responseManager;

    UpdaterListener(App app) {
        this.responseManager = app.getResponseManager();
    }

    @Override
    public void onMessageReceived(@NotNull MessageCreatedEvent event, @NotNull Responder res) {
        this.responseManager.handle(event);
    }
}
