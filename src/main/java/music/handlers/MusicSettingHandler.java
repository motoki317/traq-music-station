package music.handlers;

import api.TraqApi;
import app.Bot;
import com.github.motoki317.traq_bot.Responder;
import com.github.motoki317.traq_bot.model.MessageCreatedEvent;
import db.model.musicSetting.MusicSetting;
import db.repository.base.MusicSettingRepository;
import music.MusicState;
import music.MusicUtils;
import music.RepeatState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static commands.BotCommand.respond;
import static commands.BotCommand.respondError;

public class MusicSettingHandler {
    private final Map<String, MusicState> states;

    private final TraqApi traqApi;
    private final MusicSettingRepository musicSettingRepository;

    public MusicSettingHandler(Bot bot, Map<String, MusicState> states) {
        this.states = states;
        this.traqApi = bot.getTraqApi();
        this.musicSettingRepository = bot.getDatabase().getMusicSettingRepository();
    }

    /**
     * Retrieves voice channel the user is in.
     * @param userId User UUID
     * @return Channel UUID. null if not found.
     */
    @Nullable
    private UUID getVoiceChannel(@NotNull UUID userId) {
        return MusicUtils.getVoiceChannel(this.traqApi, userId);
    }

    @Nullable
    private MusicState getMusicState(UUID vcId) {
        synchronized (states) {
            return states.getOrDefault(vcId.toString(), null);
        }
    }

    /**
     * Retrieves setting for the vc.
     * @param vcId Voice channel ID.
     * @return Music setting. Default if not found.
     */
    @NotNull
    private MusicSetting getSetting(UUID vcId) {
        MusicState state = getMusicState(vcId);
        if (state != null) {
            return state.getSetting();
        }
        MusicSetting setting = this.musicSettingRepository.findOne(vcId::toString);
        return setting != null ? setting : MusicSetting.getDefault(vcId.toString());
    }

    /**
     * Saves setting for the guild.
     * @param setting Setting.
     * @return {@code true} if success.
     */
    private boolean saveSetting(MusicSetting setting) {
        return this.musicSettingRepository.exists(setting)
                ? this.musicSettingRepository.update(setting)
                : this.musicSettingRepository.create(setting);
    }

    private interface MusicSettingSubCommandHandler {
        void handle(MessageCreatedEvent event, Responder res, String[] args, @NotNull UUID vcId, @Nullable MusicState state);
    }

    private MusicSubCommandHandler retrieveMusicState(MusicSettingSubCommandHandler next) {
        return (event, res, args) -> {
            UUID userId = UUID.fromString(event.getMessage().getUser().getId());
            UUID vcId = getVoiceChannel(userId);
            MusicState state = null;
            if (vcId != null) {
                state = getMusicState(vcId);
            } else {
                vcId = UUID.fromString(event.getMessage().getChannelId());
            }
            next.handle(event, res, args, vcId, state);
        };
    }

    private final static int MAX_VOLUME = 150;

    private static String volumeHelp(int currentVolume) {
        return String.format("Volume Setting\n" +
                "Adjusts the player's volume.\n" +
                "Current: %s%%\n" +
                "Update: `m volume <percentage>`\n" +
                "e.g. `m volume 50`",
                currentVolume);
    }

    public MusicSubCommandHandler handleVolume = retrieveMusicState(this::_handleVolume);

    /**
     * Handles "volume" command.
     * @param event Event.
     * @param res Responder.
     * @param args Command arguments.
     * @param vcId Voice channel ID.
     * @param state Music state.
     */
    private void _handleVolume(MessageCreatedEvent event, Responder res, String[] args, UUID vcId, @Nullable MusicState state) {
        MusicSetting setting = state != null ? state.getSetting() : getSetting(vcId);
        int oldVolume = setting.getVolume();

        if (args.length <= 2) {
            respond(res, volumeHelp(oldVolume));
            return;
        }

        int newVolume;
        try {
            // accepts formats such as "50%" and "50"
            Matcher m = Pattern.compile("(\\d+)%").matcher(args[2]);
            if (m.matches()) {
                newVolume = Integer.parseInt(m.group(1));
            } else {
                newVolume = Integer.parseInt(args[2]);
            }
            if (newVolume < 0 || MAX_VOLUME < newVolume) {
                respond(res, String.format("Please input a number between 0 and %s for the new volume!", MAX_VOLUME));
                return;
            }
        } catch (NumberFormatException e) {
            respond(res, "Please input a valid number for the new volume!");
            return;
        }

        setting.setVolume(newVolume);
        if (state != null) {
            state.getPlayer().setVolume(newVolume);
        }

        if (this.saveSetting(setting)) {
            respond(res, String.format("Set volume from `%s%%` to `%s%%`!", oldVolume, newVolume));
        } else {
            respondError(res, "Something went wrong while saving setting...");
        }
    }

    private static String repeatHelp(RepeatState current) {
        return String.format("Repeat Setting\n" +
                "Sets the repeat mode.\n" +
                "Current: %s\n" +
                "Update: `m repeat <mode>`\n" +
                "Available Modes: %s",
                current.name(),
                Arrays.stream(RepeatState.values())
                        .map(r -> String.format("**%s** : %s", r.name(), r.getDescription()))
                        .collect(Collectors.joining("\n")));
    }

    public MusicSubCommandHandler handleRepeat = retrieveMusicState((event, res, args, vcId, state) -> _handleRepeat(res, args, vcId));

    /**
     * Handles "repeat" command.
     * @param res Responder.
     * @param args Command arguments.
     * @param vcId Voice channel ID.
     */
    private void _handleRepeat(Responder res, String[] args, UUID vcId) {
        MusicSetting setting = getSetting(vcId);
        RepeatState oldState = setting.getRepeat();

        if (args.length <= 2) {
            respond(res, repeatHelp(oldState));
            return;
        }

        RepeatState newState;
        try {
            newState = RepeatState.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            respond(res, "Invalid repeat type.");
            return;
        }

        setting.setRepeat(newState);

        if (this.saveSetting(setting)) {
            respond(res, String.format("Set repeat mode from `%s` to `%s`!", oldState.name(), newState.name()));
        } else {
            respondError(res, "Something went wrong while saving setting...");
        }
    }

    private String settingHelp(MusicSetting current) {
        return String.format("Music Other Settings\n" +
                "Use the command written below each option to set options.\n" +
                "\n" +
                "Show Now Playing Messages\n" +
                "Current: **%s**\n" +
                "`m setting shownp <ON/OFF>`",
                current.isShowNp() ? "ON" : "OFF");
    }

    public MusicSubCommandHandler handleSetting = retrieveMusicState((event, res, args, vcId, state) -> _handleSetting(res, args, vcId));

    /**
     * Handles "setting" command.
     * @param res Responder.
     * @param args Command arguments.
     * @param vcId Voice channel ID.
     */
    private void _handleSetting(Responder res, String[] args, UUID vcId) {
        MusicSetting setting = getSetting(vcId);

        if (args.length <= 2) {
            respond(res, settingHelp(setting));
            return;
        }

        if ("shownp".equals(args[2].toLowerCase())) {
            handleShowNP(res, setting, args);
            return;
        }

        respond(res, "Unknown music setting.");
    }

    private static String showNPHelp(boolean current) {
        return String.format("Music Other Settings - Show Now Playing Messages\n" +
                "If enabled, it will send a message of the song info on each start.\n" +
                "Default: ON\n" +
                "Current: %s\n" +
                "Update: `m setting shownp <ON/OFF>`", current ? "ON" : "OFF");
    }

    /**
     * Handles "setting shownp" command.
     * @param res Responder.
     * @param setting Current setting.
     * @param args Command arguments.
     */
    private void handleShowNP(Responder res, MusicSetting setting, String[] args) {
        if (args.length <= 3) {
            respond(res, showNPHelp(setting.isShowNp()));
            return;
        }

        boolean newValue;
        switch (args[3].toUpperCase()) {
            case "ON":
                newValue = true;
                break;
            case "OFF":
                newValue = false;
                break;
            default:
                respond(res, "Input either `ON` OR `OFF` for the new value!");
                return;
        }

        setting.setShowNp(newValue);

        if (this.saveSetting(setting)) {
            respond(res, String.format("The bot will %s display now playing messages!", newValue ? "now" : "no longer"));
        } else {
            respondError(res, "Something went wrong while saving setting...");
        }
    }
}
