package music.handlers;

import app.Responder;
import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;

public interface MusicSubCommandHandler {
    void handle(MessageCreatedEvent event, Responder res, String[] args);
}
