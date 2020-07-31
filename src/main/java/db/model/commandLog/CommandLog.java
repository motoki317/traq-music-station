package db.model.commandLog;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class CommandLog implements CommandLogId {
    private int id;
    @NotNull
    private final String kind;
    @NotNull
    private final String full;
    @NotNull
    private final String channelId;
    @NotNull
    private final String userId;
    @NotNull
    private final Date createdAt;

    // For select
    public CommandLog(int id, @NotNull String kind, @NotNull String full, @NotNull String channelId, @NotNull String userId, @NotNull Date createdAt) {
        this.id = id;
        this.kind = kind;
        this.full = full;
        this.channelId = channelId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    // For insert
    public CommandLog(@NotNull String kind, @NotNull String full, @NotNull String channelId, @NotNull String userId, @NotNull Date createdAt) {
        this.kind = kind;
        this.full = full;
        this.channelId = channelId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    @Override
    public int getId() {
        return id;
    }

    @NotNull
    public String getKind() {
        return kind;
    }

    @NotNull
    public String getFull() {
        return full;
    }

    public @NotNull String getChannelId() {
        return channelId;
    }

    public @NotNull String getUserId() {
        return userId;
    }

    @NotNull
    public Date getCreatedAt() {
        return createdAt;
    }
}
