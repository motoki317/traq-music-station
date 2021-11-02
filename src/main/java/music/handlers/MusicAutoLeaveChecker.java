package music.handlers;

import api.TraqApi;
import app.App;
import com.github.motoki317.traq4j.model.WebRTCUserState;
import db.model.musicInterruptedChannel.MusicInterruptedChannel;
import db.repository.base.MusicInterruptedChannelRepository;
import log.Logger;
import music.MusicState;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MusicAutoLeaveChecker {
    private final Map<String, MusicState> states;

    private final String botUserId;
    private final TraqApi traqApi;
    private final Logger logger;
    private final MusicInterruptedChannelRepository interruptedGuildRepository;

    private final MusicPlayHandler playHandler;

    public MusicAutoLeaveChecker(App app, Map<String, MusicState> states, MusicPlayHandler playHandler) {
        this.states = states;
        this.botUserId = app.getProperties().botUserId;
        this.traqApi = app.getTraqApi();
        this.logger = app.getLogger();
        this.interruptedGuildRepository = app.getDatabase().getMusicInterruptedChannelRepository();
        this.playHandler = playHandler;
    }

    private final static long TIME_TILL_LEAVE = TimeUnit.MINUTES.toMillis(3);

    /**
     * Checks if the bot can safely leave the voice channel.
     * When one of the following is satisfied:
     * 1. If there are no songs in the queue
     * 2. No one's listening to the music
     * @return If the bot should leave the vc.
     */
    private boolean canSafelyLeave(UUID vcId, MusicState state) {
        if (System.currentTimeMillis() - state.getLastInteract() < TIME_TILL_LEAVE) {
            return false;
        }

        if (state.getCurrentQueue().getQueue().isEmpty()) {
            return true;
        }

        List<WebRTCUserState> states = this.traqApi.getWebRTCState();
        if (states == null) {
            return false;
        }
        int listeningCount = 0;
        for (WebRTCUserState rtcState : states) {
            if (!rtcState.getChannelId().equals(vcId)) continue;
            if (rtcState.getUserId().toString().equals(this.botUserId)) continue;
            listeningCount++;
        }

        return listeningCount <= 0;
    }

    private void shutdownGuild(UUID vcId, MusicState state) {
        try {
            this.playHandler.shutdownPlayer(true, vcId, state);
        } catch (RuntimeException e) {
            this.logger.logException("Something went wrong while shutting down guild music", e);
        }
    }

    void checkAllGuilds() {
        synchronized (states) {
            for (Iterator<Map.Entry<String, MusicState>> iterator = states.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry<String, MusicState> entry = iterator.next();
                UUID vcId = UUID.fromString(entry.getKey());
                MusicState state = entry.getValue();

                if (!canSafelyLeave(vcId, state)) {
                    continue;
                }

                shutdownGuild(vcId, state);

                this.traqApi.sendMessage(UUID.fromString(state.getTextChannelId()),
                        "Left the voice channel due to inactivity.", true);

                iterator.remove();
            }
        }
    }

    /**
     * Shuts down all music players.
     * To be used on bot shutdown.
     */
    public void forceShutdownAllGuilds() {
        synchronized (states) {
            List<MusicInterruptedChannel> toSave = new ArrayList<>(states.size());

            for (Map.Entry<String, MusicState> entry : states.entrySet()) {
                UUID vcId = UUID.fromString(entry.getKey());
                MusicState state = entry.getValue();
                shutdownGuild(vcId, state);
                toSave.add(new MusicInterruptedChannel(vcId.toString(), state.getTextChannelId()));
            }

            boolean res = this.interruptedGuildRepository.createAll(toSave);
            if (!res) {
                this.logger.log("Music leave: Failed to insert into interrupted guilds");
            }

            states.clear();
        }
    }
}
