package api;

import com.github.motoki317.traq4j.model.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public interface TraqApi {
    @Nullable
    Message sendMessage(UUID channelId, String message, boolean embed);
    void editMessage(UUID messageId, String message);
    @Nullable
    Channel getChannelByID(UUID id);
    @Nullable
    User getUserByID(UUID id);
    @Nullable
    List<WebRTCUserState> getWebRTCState();
    @Nullable
    WebRTCAuthenticateResult authenticateWebRTC(String peerId);
}
