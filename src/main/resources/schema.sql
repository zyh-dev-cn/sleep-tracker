CREATE DATABASE IF NOT EXISTS sleep_tracker
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_general_ci;

USE sleep_tracker;

CREATE TABLE IF NOT EXISTS sleep_record (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL DEFAULT 1,
    bed_time   DATETIME     NULL,
    sleep_time DATETIME     NULL,
    wake_time  DATETIME     NULL,
    wake_count INT          NOT NULL DEFAULT 0,
    quality    TINYINT      NULL,
    note       VARCHAR(255) NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_wake (user_id, wake_time)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
