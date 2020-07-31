package app;

import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import commands.*;
import db.model.commandLog.CommandLog;
import db.repository.base.CommandLogRepository;
import log.Logger;
import log.SpamChecker;
import music.Music;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static commands.BotCommand.respond;
import static commands.BotCommand.respondError;

public class CommandListener implements EventListener {
    private final List<BotCommand> commands;
    private final Map<String, BotCommand> commandNameMap;
    private int maxArgumentsLength;

    private final ExecutorService threadPool;

    private final Logger logger;
    private final SpamChecker spamChecker;
    private final String defaultPrefix;

    private final CommandLogRepository commandLogRepository;

    CommandListener(Bot bot) {
        this.commands = new ArrayList<>();
        this.commandNameMap = new HashMap<>();
        this.maxArgumentsLength = 1;

        this.threadPool = Executors.newFixedThreadPool(5);

        this.logger = bot.getLogger();
        this.spamChecker = new SpamChecker();
        this.defaultPrefix = bot.getProperties().prefix;

        this.commandLogRepository = bot.getDatabase().getCommandLogRepository();

        registerCommands(bot);
    }

    @SuppressWarnings({"OverlyLongMethod", "OverlyCoupledMethod"})
    private void registerCommands(Bot bot) {
        addCommand(new Help(bot, this.commands, this.commandNameMap, () -> this.maxArgumentsLength));
        addCommand(new CommandAliases(this.commandNameMap, () -> this.maxArgumentsLength));
        addCommand(new Ping());
        addCommand(new Info());

        addCommand(new Music(bot));
    }

    private void addCommand(BotCommand command) {
        for (String commandName : command.getNames()) {
            commandName = commandName.toLowerCase();

            if (this.commandNameMap.containsKey(commandName)) {
                throw new Error("FATAL: Command name conflict: " + commandName + "\n" +
                        "Command " + command.getClass().getName() + " is not being added.");
            }

            this.commandNameMap.put(commandName, command);
        }
        this.commands.add(command);
        this.maxArgumentsLength = Math.max(this.maxArgumentsLength, command.getArgumentsLength());
    }

    @Override
    public void onMessageReceived(@NotNull MessageCreatedEvent event, @NotNull Responder res) {
        // Check prefix
        String rawMessage = event.getMessage().getPlainText();
        if (!rawMessage.startsWith(defaultPrefix)) return;

        String commandMessage = rawMessage.substring(defaultPrefix.length());
        String[] args = commandMessage.split("\\s+");

        // Process command from the most 'specific' (e.g. g pws) to most 'generic' (e.g. guild)
        for (int argLength = Math.min(this.maxArgumentsLength, args.length); argLength > 0; argLength--) {
            String cmdBase = String.join(" ", Arrays.copyOfRange(args, 0, argLength));
            // Command name match
            if (this.commandNameMap.containsKey(cmdBase.toLowerCase())) {
                int finalArgLength = argLength;
                this.threadPool.execute(() -> processCommand(event, res, commandMessage, args, finalArgLength, cmdBase));
                return;
            }
        }
    }

    private void processCommand(@Nonnull MessageCreatedEvent event, Responder res, String commandMessage, String[] args, int argLength, String cmdBase) {
        BotCommand command = this.commandNameMap.get(cmdBase.toLowerCase());

        // Check guild-only command -> TODO

        // Check spam and log event
        boolean isSpam = this.spamChecker.isSpam(event);
        this.logger.logEvent(event, isSpam);
        if (isSpam) {
            String message = "You are requesting commands too quickly! Please wait at least 1 second between each commands.";
            respond(res, message);
            return;
        }

        // If the first argument is "help", then send full help of the command
        // e.g. "track help"
        if (argLength < args.length && args[argLength].equalsIgnoreCase("help")) {
            respond(res, command.longHelp());
            return;
        }

        // Process command
        try {
            command.process(event, res, args);
        } catch (Throwable e) {
            respondError(res, "Something went wrong while processing your command...");
            this.logger.logException("Something went wrong while processing a user command", e);
        }

        addCommandLog(cmdBase, commandMessage, event);
    }

    /**
     * Adds command log to db.
     */
    private void addCommandLog(String kind, String full, MessageCreatedEvent event) {
        Date createdAt = new Date(
                event.getBasePayload().getEventTime().atOffset(ZoneOffset.UTC).toEpochSecond() * 1000
        );
        CommandLog entity = new CommandLog(kind, full, event.getMessage().getChannelId(), event.getMessage().getUser().getId(), createdAt);
        if (!this.commandLogRepository.create(entity)) {
            this.logger.log("Failed to log command to db.");
        }
    }
}
