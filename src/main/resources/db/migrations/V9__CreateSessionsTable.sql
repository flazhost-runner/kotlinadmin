CREATE TABLE IF NOT EXISTS sessions (
    id          VARCHAR(128) PRIMARY KEY,
    data        TEXT         NOT NULL,
    expires_at  DATETIME     NOT NULL
);
