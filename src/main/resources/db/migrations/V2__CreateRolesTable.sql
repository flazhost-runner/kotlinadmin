CREATE TABLE IF NOT EXISTS roles (
    id         VARCHAR(36)  NOT NULL,
    name       VARCHAR(255) NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'Active',
    "desc"     VARCHAR(255),
    created_by VARCHAR(36),
    updated_by VARCHAR(36),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT roles_name_unique UNIQUE (name)
);
