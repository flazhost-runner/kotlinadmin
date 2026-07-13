CREATE TABLE IF NOT EXISTS sessions (
    id          VARCHAR(128) NOT NULL,
    data        TEXT         NOT NULL,
    -- TIMESTAMP, bukan DATETIME: PostgreSQL tidak punya tipe DATETIME.
    --
    -- DEFAULT wajib DISEBUT, walau aplikasi selalu mengisi expires_at sendiri. Tanpa itu,
    -- MySQL (explicit_defaults_for_timestamp=OFF — setelan RDS yang kami pakai) diam-diam
    -- menjadikan kolom TIMESTAMP pertama sebagai
    --     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    -- sehingga masa berlaku sesi ter-reset ke "sekarang" setiap baris di-update — sesi tidak
    -- pernah benar-benar kedaluwarsa di MySQL, padahal di SQLite/PostgreSQL kedaluwarsa.
    -- Menyebut DEFAULT (tanpa ON UPDATE) mematikan perilaku implisit itu. Kalau suatu saat
    -- ada insert yang lupa mengisinya, sesi lahir langsung kedaluwarsa — gagal ke arah aman.
    expires_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
