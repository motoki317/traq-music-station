package api;

import com.github.motoki317.traq4j.ApiClient;
import com.github.motoki317.traq4j.ApiException;
import com.github.motoki317.traq4j.api.*;
import com.github.motoki317.traq4j.model.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TraqApiImpl implements TraqApi {
    private final MessageApi messageApi;
    private final ChannelApi channelApi;
    private final UserApi userApi;
    private final WebrtcApi webrtcApi;

    private Map<UUID, User> usersCache;

    /**
     * Creates new traQ API client.
     * @param basePath Base path. e.g. "http://q.trap.jp/api/v3"
     * @param accessToken Bot access token.
     */
    public TraqApiImpl(@Nullable String basePath, String accessToken) {
        ApiClient client = new ApiClient();
        if (basePath != null) {
            client.setBasePath(basePath);
        }
        client.addDefaultHeader("Authorization", "Bearer " + accessToken);

        this.messageApi = new MessageApi(client);
        this.channelApi = new ChannelApi(client);
        this.userApi = new UserApi(client);
        this.webrtcApi = new WebrtcApi(client);
    }

    private static void handleError(ApiException e) {
        e.printStackTrace();
        System.out.printf("code: %s, body: %s\n", e.getCode(), e.getResponseBody());
    }

    @Nullable
    @Override
    public Message sendMessage(UUID channelId, String message, boolean embed) {
        try {
            return messageApi.postMessage(
                    channelId,
                    new PostMessageRequest().content(message).embed(embed)
            );
        } catch (ApiException e) {
            handleError(e);
            return null;
        }
    }

    @Override
    public void editMessage(UUID messageId, String message) {
        try {
            messageApi.editMessage(
                    messageId,
                    new PostMessageRequest().content(message).embed(true)
            );
        } catch (ApiException e) {
            handleError(e);
        }
    }

    @Nullable
    @Override
    public Channel getChannelByID(UUID id) {
        // TODO: maybe cache
        try {
            return channelApi.getChannel(id);
        } catch (ApiException e) {
            handleError(e);
            return null;
        }
    }

    @Nullable
    @Override
    public User getUserByID(UUID id) {
        if (this.usersCache == null) {
            List<User> users;
            try {
                users = userApi.getUsers(false);
            } catch (ApiException e) {
                handleError(e);
                return null;
            }
            this.usersCache = new HashMap<>();
            for (User user : users) {
                this.usersCache.put(user.getId(), user);
            }
        }

        return this.usersCache.get(id);
    }

    @Nullable
    @Override
    public List<WebRTCUserState> getWebRTCState() {
        try {
            return webrtcApi.getWebRTCState();
        } catch (ApiException e) {
            handleError(e);
            return null;
        }
    }

    @Nullable
    @Override
    public WebRTCAuthenticateResult authenticateWebRTC(String peerId) {
        try {
            return webrtcApi.postWebRTCAuthenticate(
                    new PostWebRTCAuthenticateRequest().peerId(peerId)
            );
        } catch (ApiException e) {
            handleError(e);
            return null;
        }
    }
}
