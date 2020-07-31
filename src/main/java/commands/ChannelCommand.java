package commands;

public abstract class ChannelCommand extends BotCommand {
    public boolean publicChannelOnly() {
        return true;
    }
}
