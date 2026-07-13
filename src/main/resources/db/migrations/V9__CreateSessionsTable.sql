CREATE TABLE IF NOT EXISTS sessions (
    id          VARCHAR(128) NOT NULL,
    data        TEXT         NOT NULL,
    -- TIMESTAMP, bukan DATETIME: PostgreSQL tidak punya tipe DATETIME.
    expires_at  TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);
