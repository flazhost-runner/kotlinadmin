CREATE TABLE IF NOT EXISTS users (
    id                   VARCHAR(36)  NOT NULL,
    code                 VARCHAR(20)  NOT NULL,
    name                 VARCHAR(50)  NOT NULL,
    phone                VARCHAR(15),
    email                VARCHAR(255) NOT NULL,
    -- NULL WAJIB DISEBUT. Ini kolom TIMESTAMP pertama di tabel, dan pada MySQL dengan
    -- explicit_defaults_for_timestamp=OFF (setelan RDS produksi) kolom TIMESTAMP pertama
    -- yang tidak menyebut NULL/DEFAULT diam-diam menjadi
    --     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP.
    -- Akibatnya: user yang belum verifikasi email tercatat SUDAH terverifikasi (NULL
    -- dipaksa jadi "sekarang"), dan tanggal verifikasi ter-reset setiap baris di-update.
    -- Tidak terlihat di SQLite/PostgreSQL — hanya rusak di MySQL.
    email_verified_at    TIMESTAMP    NULL DEFAULT NULL,
    password             VARCHAR(255) NOT NULL,
    password_otp         VARCHAR(255),
    password_otp_expires BIGINT,
    status               VARCHAR(20)  NOT NULL DEFAULT 'Active',
    picture              VARCHAR(255),
    blocked              BOOLEAN      NOT NULL DEFAULT FALSE,
    blocked_reason       VARCHAR(255),
    timezone             VARCHAR(255) NOT NULL DEFAULT 'UTC',
    created_by           VARCHAR(36),
    updated_by           VARCHAR(36),
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT users_code_unique  UNIQUE (code),
    CONSTRAINT users_email_unique UNIQUE (email)
);
