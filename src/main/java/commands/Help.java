package commands;

import app.Bot;
import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Help extends GenericCommand {
    private final Bot bot;

    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private final Supplier<Integer> maxArgumentsLength;

    public Help(Bot bot, List<BotCommand> commands, Map<String, BotCommand> commandNameMap, Supplier<Integer> maxArgumentsLength) {
        this.bot = bot;
        this.commands = commands;
        this.commandNameMap = commandNameMap;
        this.maxArgumentsLength = maxArgumentsLength;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"help", "h"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "help [cmd name]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Calls this help. Use with arguments to view detailed help of each command. e.g. `help ping`";
    }

    @NotNull
    @Override
    public String longHelp() {
        return "Use help command with arguments to view detailed help of each command. e.g. `help ping`";
    }

    @Override
    public void process(@NotNull MessageCreatedEvent event, @NotNull Responder res, @NotNull String[] args) {
        if (args.length == 1) {
            if (this.maxPage() == 0) {
                respond(res, this.getPage());
                return;
            }

            this.getPage();
            return;
        }

        // Supports nested command help (e.g. ">help guild levelRank")
        args = Arrays.copyOfRange(args, 1, args.length);
        for (int argLength = Math.min(this.maxArgumentsLength.get(), args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                BotCommand cmd = this.commandNameMap.get(cmdBase.toLowerCase());
                respond(res, cmd.longHelp());
                return;
            }
        }

        respond(res, "Command not found, try `help`.");
    }

    private static final int COMMANDS_PER_PAGE = 5;

    private String getPage() {
        List<String> ret = new ArrayList<>();
        ret.add("## Commands List");

        int min = 0;
        int max = this.commands.size();
        for (int i = min; i < max; i++) {
            BotCommand cmd = this.commands.get(i);
            ret.add(String.format("### %s\n" +
                    "%s", this.bot.getProperties().prefix + cmd.syntax(),
                    cmd.shortHelp()));
        }

        ret.add("");
        ret.add("<text> means required, and [text] means optional arguments.");

        return String.join("\n", ret);
    }

    private int maxPage() {
        return (this.commands.size() - 1) / COMMANDS_PER_PAGE;
    }
}
