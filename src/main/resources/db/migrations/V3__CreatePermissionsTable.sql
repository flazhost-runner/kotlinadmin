CREATE TABLE IF NOT EXISTS permissions (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    guard_name VARCHAR(20)  NOT NULL DEFAULT 'web',
    method     VARCHAR(255),
    status     VARCHAR(20)  NOT NULL DEFAULT 'Active',
    "desc"     VARCHAR(255),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id)
);

CREATE INDEX IF NOT EXISTS permissions_name_idx       ON permissions (name);
CREATE INDEX IF NOT EXISTS permissions_guard_name_idx ON permissions (guard_name);
