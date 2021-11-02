package commands;

import api.TraqApi;
import app.Responder;
import com.github.motoki317.traq4j.ApiException;
import com.github.motoki317.traq4j.model.Message;
import com.github.motoki317.traq_ws_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public abstract class BotCommand {
    public abstract boolean publicChannelOnly();

    /**
     * Command names including aliases. Used to process command inputs.
     * Examples:
     * {{"help", "h"}} for 1-argument command.
     * {{"guild", "g"}, {"levelRank", "lRank"}} for 2-arguments command.
     * @return Command names.
     */
    @NotNull
    protected abstract String[][] names();

    /**
     * Get names for this command including aliases.
     * Examples:
     * {"help", "h"} for 1-argument command.
     * {"guild levelRank", "guild lRank", "g levelRank", "g lRank"} for 2-arguments command.
     * @return Names. Values possibly includes spaces.
     */
    public Set<String> getNames() {
        return getNamesRec(this.names(), 0);
    }

    private static Set<String> getNamesRec(String[][] base, int i) {
        if (base.length - 1 == i) {
            return new HashSet<>(Arrays.asList(base[i]));
        }

        Set<String> ret = new HashSet<>();
        for (String latter : getNamesRec(base, i + 1)) {
            for (String current : base[i]) {
                ret.add(current + " " + latter);
            }
        }
        return ret;
    }

    /**
     * Get base arguments length of this command.
     * Examples:
     * "help" command: 1,
     * "g levelRank" command: 2
     * @return Length of base arguments.
     */
    public int getArgumentsLength() {
        return this.names().length;
    }

    /**
     * Command syntax. Used in help display.
     * Example:
     * "help [command name]"
     * @return Command syntax.
     */
    @NotNull
    public abstract String syntax();

    /**
     * Shows short help in help command.
     * @return Short help.
     */
    @NotNull
    public abstract String shortHelp();

    /**
     * Shows long help in help (cmd name) command.
     * @return Long help message.
     */
    @NotNull
    public abstract String longHelp();

    /**
     * Process a command.
     * @param event Message received event.
     * @param res Responder.
     * @param args Argument array, separated by space characters.
     */
    public abstract void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args);

    public static void respond(Responder responder, String msg) {
        try {
            responder.respond(msg, true);
        } catch (ApiException e) {
            e.printStackTrace();
            System.out.printf("code: %s, body: %s\n", e.getCode(), e.getResponseBody());
        }
    }

    public static void respond(TraqApi traqApi, MessageCreatedEvent event, String msg) {
        traqApi.sendMessage(UUID.fromString(event.message().channelId()), msg, true);
    }

    public static void respond(TraqApi traqApi, UUID channelId, String msg) {
        traqApi.sendMessage(channelId, msg, true);
    }

    public static void respond(TraqApi traqApi, UUID channelId, String msg, Consumer<Message> onSuccess) {
        Message success = traqApi.sendMessage(channelId, msg, true);
        if (success == null) {
            return;
        }
        onSuccess.accept(success);
    }

    public static void respondError(Responder res, String msg) {
        respond(res, "An error occurred: " + msg);
    }
}
