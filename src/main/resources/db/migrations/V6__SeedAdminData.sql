-- Seed idempoten lintas dialek.
--
-- `INSERT OR IGNORE` hanya ada di SQLite; MySQL punya `INSERT IGNORE` dan
-- PostgreSQL punya `ON CONFLICT DO NOTHING` — ketiganya saling tidak kompatibel.
-- Bentuk `INSERT ... SELECT ... FROM (SELECT 1) AS seed WHERE NOT EXISTS (...)`
-- adalah SQL standar yang dipahami ketiga database, dan tetap no-op saat baris
-- sudah ada (mis. Flyway repair / DB lama).
--
-- Derived table `(SELECT 1) AS seed` wajib: MySQL & PostgreSQL menolak klausa
-- WHERE pada SELECT tanpa FROM.

-- Seed: Administrator role
INSERT INTO roles (id, name, status, "desc", created_by, updated_by, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000001',
    'Administrator',
    'Active',
    '',
    'system',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (SELECT 1) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE id = '00000000-0000-0000-0000-000000000001'
);

-- Seed: admin user (password = 12345678, bcrypt 10 rounds — matches NodeAdmin BCRYPT_ROUNDS=10)
INSERT INTO users (id, code, name, phone, email, email_verified_at, password, status, timezone, blocked, blocked_reason, created_by, updated_by, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000002',
    '0000000001',
    'Administrator',
    '12345678910',
    'admin@admin.com',
    CURRENT_TIMESTAMP,
    '$2a$10$s6QlWQGn5lk.vHJLgereKOnw1RrLDfpDsQvZRXEufTDhyTHSO19oa',
    'Active',
    'Asia/Jakarta',
    -- FALSE, bukan 0: PostgreSQL menolak integer untuk kolom BOOLEAN.
    FALSE,
    '',
    'system',
    'system',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (SELECT 1) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM users WHERE id = '00000000-0000-0000-0000-000000000002'
);

-- Assign Administrator role to admin user
INSERT INTO users_roles (user_id, role_id)
SELECT
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000001'
FROM (SELECT 1) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM users_roles
    WHERE user_id = '00000000-0000-0000-0000-000000000002'
      AND role_id = '00000000-0000-0000-0000-000000000001'
);

-- Seed: default settings row
INSERT INTO settings (id, name, theme, fe_template, created_by, updated_by, created_at, updated_at)
SELECT
    '00000000-0000-0000-0000-000000000003',
    'KotlinAdmin',
    'Blue',
    'agency-consulting-002-creative-agency',
    '00000000-0000-0000-0000-000000000002',
    '00000000-0000-0000-0000-000000000002',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM (SELECT 1) AS seed
WHERE NOT EXISTS (
    SELECT 1 FROM settings WHERE id = '00000000-0000-0000-0000-000000000003'
);
