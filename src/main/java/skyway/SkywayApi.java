package skyway;

import java.util.UUID;

public interface SkywayApi {
    /**
     * joinChannel joins the actual voice channel via Skyway API.
     * @param audioOrigin Audio origin URL.
     * @param channelId Voice channel ID.
     */
    void joinChannel(String audioOrigin, UUID channelId);
    void close(UUID channelID);
}
