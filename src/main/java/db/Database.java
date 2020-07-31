package db;

import db.repository.base.*;
import org.jetbrains.annotations.NotNull;

public interface Database {
    @NotNull
    CommandLogRepository getCommandLogRepository();
    @NotNull
    MusicSettingRepository getMusicSettingRepository();
    @NotNull
    MusicQueueRepository getMusicQueueRepository();
    @NotNull
    MusicInterruptedChannelRepository getMusicInterruptedChannelRepository();
}
