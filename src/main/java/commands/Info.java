package commands;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

public class Info extends GenericCommand {
    public Info() {
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"info"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "info";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Shows this bot's meta info.";
    }

    @NotNull
    @Override
    public String longHelp() {
        return "Shows this bot's meta info; version, wiki link etc.";
    }

    @Override
    public void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args) {
        respond(res, getInfo());
    }

    private String getInfo() {
        return "Music bot - work in progress!";
    }
}
