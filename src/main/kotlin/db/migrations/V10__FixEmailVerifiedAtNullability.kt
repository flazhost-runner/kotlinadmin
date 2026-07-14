package db.migrations

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context

/**
 * Pulihkan `users.email_verified_at` pada database MySQL yang sudah terlanjur dibuat.
 *
 * V1 lama menulis kolom itu sebagai `TIMESTAMP,` tanpa menyebut NULL. Pada MySQL dengan
 * `explicit_defaults_for_timestamp=OFF` (setelan RDS produksi), kolom TIMESTAMP pertama
 * yang tidak menyebut NULL/DEFAULT diam-diam menjadi
 * `NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP` — sehingga user yang
 * belum verifikasi email tercatat sudah terverifikasi, dan tanggalnya ter-reset setiap
 * baris di-update. V1 sudah diperbaiki untuk instalasi baru, tapi database yang sudah
 * bermigrasi tidak ikut berubah — itulah tugas migrasi ini.
 *
 * Ditulis sebagai Java-migration, bukan SQL, karena perbaikannya khusus MySQL: SQLite tidak
 * punya ALTER COLUMN sama sekali, dan di PostgreSQL kolomnya memang sudah benar.
 */
@Suppress("ClassNaming", "ClassName")
class V10__FixEmailVerifiedAtNullability : BaseJavaMigration() {

    override fun migrate(context: Context) {
        val connection = context.connection
        val product = connection.metaData.databaseProductName.orEmpty()
        val isMysql = product.contains("MySQL", ignoreCase = true) ||
            product.contains("MariaDB", ignoreCase = true)
        if (!isMysql) return

        connection.createStatement().use { statement ->
            statement.execute(
                "ALTER TABLE users MODIFY email_verified_at TIMESTAMP NULL DEFAULT NULL"
            )
        }
    }
}
