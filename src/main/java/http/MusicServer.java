package http;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import java.util.UUID;

public interface MusicServer {
    /**
     * Prepares music server
     * @param peerId Peer ID for skyway
     * @param player Audio player
     * @param channelId Voice channel ID.
     * @return The URL the browser should next connect to.
     */
    String serve(String peerId, AudioPlayer player, UUID channelId);
    void stop(UUID channelId);
}
