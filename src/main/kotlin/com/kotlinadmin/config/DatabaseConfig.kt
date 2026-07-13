package com.kotlinadmin.config

import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.RolesPermissions
import com.kotlinadmin.modules.access.models.Users
import com.kotlinadmin.modules.access.models.UsersRoles
import com.kotlinadmin.modules.setting.models.Settings
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import javax.sql.DataSource

object DatabaseConfig {
    private const val CONNECTION_TIMEOUT_MS = 10_000L
    private const val SQLITE_MAX_POOL = 1

    fun setup(config: DbConfig) {
        // Flyway & Exposed berbagi satu pool: jatah koneksi DB managed sangat ketat
        // (tier terkecil = 2, dibagi ke SEMUA replika), jadi migrasi tidak boleh
        // membuka koneksi sendiri di luar batas pool.
        val dataSource = buildDataSource(config)
        runFlyway(dataSource)
        Database.connect(dataSource)
    }

    private fun buildDataSource(config: DbConfig): HikariDataSource {
        val isSqlite = config.driver.contains("sqlite", ignoreCase = true)
        val hikari = HikariConfig().apply {
            jdbcUrl = config.url
            driverClassName = config.driver
            username = config.user
            password = config.password
            // SQLite mengunci seluruh file per penulis; >1 koneksi hanya menghasilkan
            // SQLITE_BUSY, bukan paralelisme.
            maximumPoolSize = if (isSqlite) SQLITE_MAX_POOL else config.maxPool
            minimumIdle = 1
            connectionTimeout = CONNECTION_TIMEOUT_MS

            // MySQL: migrasi mengutip identifier reserved (mis. "desc") dengan kutip ganda
            // ANSI, sedangkan default sql_mode MySQL membacanya sebagai literal string.
            // ANSI_QUOTES membuatnya dikenali sebagai nama kolom (Postgres/SQLite sudah ANSI).
            // Backtick milik Exposed tetap valid saat ANSI_QUOTES aktif.
            if (config.driver.contains("mysql", ignoreCase = true)) {
                connectionInitSql = "SET SESSION sql_mode = CONCAT(@@sql_mode, ',ANSI_QUOTES')"
            }
        }
        return HikariDataSource(hikari)
    }

    private fun runFlyway(dataSource: DataSource) {
        val flyway = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migrations")
            .baselineOnMigrate(true)
            .load()
        // V3/V6/V9 ditulis ulang dari dialek SQLite ke SQL portabel. Isinya setara
        // untuk DB yang sudah terlanjur bermigrasi, tapi checksum-nya berubah → tanpa
        // repair(), Flyway menolak start dengan "Migration checksum mismatch" di setiap
        // DB SQLite lama. repair() menyelaraskan checksum di flyway_schema_history dan
        // no-op pada DB baru.
        flyway.repair()
        flyway.migrate()
    }

    suspend fun <T> dbQuery(block: Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, statement = block)
}

// Also expose as top-level for SettingTable.kt which imports it directly
suspend fun <T> dbQuery(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

val ALL_TABLES = arrayOf(Users, Roles, Permissions, Settings, UsersRoles, RolesPermissions)
