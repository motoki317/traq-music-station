package music.handlers;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;

public interface MusicSubCommandHandler {
    void handle(MessageCreatedEvent event, Responder res, String[] args);
}
