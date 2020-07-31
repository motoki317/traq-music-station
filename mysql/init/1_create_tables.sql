CREATE TABLE IF NOT EXISTS `command_log` (
    `id` INT AUTO_INCREMENT PRIMARY KEY NOT NULL,
    `kind` VARCHAR(30) NOT NULL,
    `full` VARCHAR(2500) NOT NULL,
    `channel_id` CHAR(36) NOT NULL,
    `user_id` CHAR(36) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT NOW(),
    KEY `kind_idx` (`kind`),
    KEY `user_id_idx` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `music_setting` (
    `channel_id` CHAR(36) PRIMARY KEY,
    `volume` INT NOT NULL,
    `repeat` VARCHAR(30) NOT NULL,
    `show_np` BOOLEAN NOT NULL,
    `restrict_channel` BIGINT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Music queue cache
CREATE TABLE IF NOT EXISTS `music_queue` (
    `channel_id` CHAR(36) NOT NULL,
    # index inside queue, 0-indexed
    `index` INT NOT NULL,
    `user_id` CHAR(36) NOT NULL,
    `url` VARCHAR(500) NOT NULL,
    `position` BIGINT NOT NULL,
    `updated_at` DATETIME DEFAULT NOW() ON UPDATE NOW(),
    UNIQUE KEY `channel_id_index_idx` (`channel_id`, `index`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

# Temporary table to hold list of guilds that were interrupted music
CREATE TABLE IF NOT EXISTS `music_interrupted_channel` (
    `vc_id` CHAR(36) NOT NULL PRIMARY KEY ,
    `text_channel_id` CHAR(36) NOT NULL
) ENGINE=InnodB DEFAULT CHARSET=utf8mb4;
