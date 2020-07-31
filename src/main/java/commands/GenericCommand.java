package commands;

public abstract class GenericCommand extends BotCommand {
    @Override
    public boolean publicChannelOnly() {
        return false;
    }
}
