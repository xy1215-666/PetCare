CREATE TABLE app_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(254) NULL,
    phone VARCHAR(32) NULL,
    user_role VARCHAR(16) NOT NULL DEFAULT 'USER',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    create_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    update_time TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    deleted TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_email UNIQUE (email),
    CONSTRAINT uk_app_user_phone UNIQUE (phone),
    CONSTRAINT chk_app_user_role CHECK (user_role IN ('USER', 'ADMIN')),
    CONSTRAINT chk_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED')),
    CONSTRAINT chk_app_user_deleted CHECK (deleted IN (0, 1))
);
