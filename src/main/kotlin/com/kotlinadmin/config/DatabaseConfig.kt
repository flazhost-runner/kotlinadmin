package com.kotlinadmin.config

import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.RolesPermissions
import com.kotlinadmin.modules.access.models.Users
import com.kotlinadmin.modules.access.models.UsersRoles
import com.kotlinadmin.modules.setting.models.Settings
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object DatabaseConfig {
    fun setup(config: DbConfig) {
        runFlyway(config)
        Database.connect(
            url = config.url,
            driver = config.driver,
            user = config.user,
            password = config.password
        )
    }

    private fun runFlyway(config: DbConfig) {
        val flyway = Flyway.configure()
            .dataSource(config.url, config.user, config.password)
            .locations("classpath:db/migrations")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()
    }

    suspend fun <T> dbQuery(block: Transaction.() -> T): T =
        newSuspendedTransaction(Dispatchers.IO, statement = block)
}

// Also expose as top-level for SettingTable.kt which imports it directly
suspend fun <T> dbQuery(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, statement = block)

val ALL_TABLES = arrayOf(Users, Roles, Permissions, Settings, UsersRoles, RolesPermissions)
