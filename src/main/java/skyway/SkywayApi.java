package skyway;

import java.util.UUID;

public interface SkywayApi {
    void joinChannel(String audioOrigin, UUID channelId);
    void close(UUID channelID);
}
