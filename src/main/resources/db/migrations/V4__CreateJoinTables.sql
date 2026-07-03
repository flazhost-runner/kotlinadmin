CREATE TABLE IF NOT EXISTS users_roles (
    user_id VARCHAR(36) NOT NULL,
    role_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id)       ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id)       ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roles_permissions (
    role_id       VARCHAR(36) NOT NULL,
    permission_id VARCHAR(36) NOT NULL,
    PRIMARY KEY (role_id, permission_id),
    FOREIGN KEY (role_id)       REFERENCES roles(id)       ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);
