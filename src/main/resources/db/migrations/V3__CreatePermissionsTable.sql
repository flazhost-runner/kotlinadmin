CREATE TABLE IF NOT EXISTS permissions (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    guard_name VARCHAR(20)  NOT NULL DEFAULT 'web',
    method     VARCHAR(255),
    status     VARCHAR(20)  NOT NULL DEFAULT 'Active',
    "desc"     VARCHAR(255),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- MySQL tidak mengenal IF NOT EXISTS pada CREATE INDEX. Flyway menjamin migrasi
-- ini hanya dieksekusi sekali, jadi penjaga tersebut memang tidak diperlukan.
CREATE INDEX permissions_name_idx       ON permissions (name);
CREATE INDEX permissions_guard_name_idx ON permissions (guard_name);
