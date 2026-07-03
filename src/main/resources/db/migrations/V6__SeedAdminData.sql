-- Seed: Administrator role
INSERT OR IGNORE INTO roles (id, name, status, "desc", created_by, updated_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'Administrator',
    'Active',
    '',
    'system',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Seed: admin user (password = 12345678, bcrypt 10 rounds — matches NodeAdmin BCRYPT_ROUNDS=10)
INSERT OR IGNORE INTO users (id, code, name, phone, email, email_verified_at, password, status, timezone, blocked, blocked_reason, created_by, updated_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '0000000001',
    'Administrator',
    '12345678910',
    'admin@admin.com',
    CURRENT_TIMESTAMP,
    '$2a$10$s6QlWQGn5lk.vHJLgereKOnw1RrLDfpDsQvZRXEufTDhyTHSO19oa',
    'Active',
    'Asia/Jakarta',
    0,
    '',
    'system',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- Assign Administrator role to admin user
INSERT OR IGNORE INTO users_roles (user_id, role_id)
VALUES (
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001'
);

-- Seed: default settings row
INSERT OR IGNORE INTO settings (id, name, theme, fe_template, created_by, updated_by, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000003',
    'KotlinAdmin',
    'Blue',
    'agency-consulting-002-creative-agency',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);
