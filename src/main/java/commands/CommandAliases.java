package commands;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CommandAliases extends GenericCommand {
    private final Map<String, BotCommand> commandNameMap;
    private final Supplier<Integer> maxArgumentsLength;

    public CommandAliases(Map<String, BotCommand> commandNameMap, Supplier<Integer> maxArgumentsLength) {
        this.commandNameMap = commandNameMap;
        this.maxArgumentsLength = maxArgumentsLength;
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"alias", "aliases"}};
    }

    @Override
    public @NotNull String syntax() {
        return "alias <command name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows all aliases of each bot command.";
    }

    @Override
    public @NotNull String longHelp() {
        return String.format("Alias Command Help\n" +
                "Syntax: %s\n" +
                "%s" +
                "Example: `alias music`, `alias help`", this.syntax(), this.shortHelp());
    }

    @Override
    public void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(res, this.longHelp());
            return;
        }

        // Supports nested command (e.g. ">alias guild levelRank")
        args = Arrays.copyOfRange(args, 1, args.length);
        for (int argLength = Math.min(this.maxArgumentsLength.get(), args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                BotCommand cmd = this.commandNameMap.get(cmdBase.toLowerCase());
                respond(res, formatMessage(cmd));
                return;
            }
        }

        respond(res, "Command not found, try `help`.");
    }

    private static String formatMessage(BotCommand cmd) {
        return "This command has following aliases:\n" +
                cmd.getNames().stream().map(n -> "`" + n + "`").collect(Collectors.joining(", "));
    }
}
