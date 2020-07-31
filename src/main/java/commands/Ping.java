package commands;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

public class Ping extends GenericCommand {
    public Ping() {
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"ping"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "ping";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Pong!";
    }

    @NotNull
    @Override
    public String longHelp() {
        return "Pong!";
    }

    @Override
    public void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args) {
        respond(res, "Pong!");
    }
}
