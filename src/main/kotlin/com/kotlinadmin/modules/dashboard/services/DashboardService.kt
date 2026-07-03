package com.kotlinadmin.modules.dashboard.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.Users
import com.kotlinadmin.modules.setting.models.SettingCache
import org.jetbrains.exposed.sql.selectAll

class DashboardService : IDashboardService {

    override suspend fun getStats(): DashboardStats {
        val (users, roles, permissions) = dbQuery {
            Triple(
                Users.selectAll().count(),
                Roles.selectAll().count(),
                Permissions.selectAll().count()
            )
        }
        val setting = SettingCache.get()
        return DashboardStats(
            totalUsers = users,
            totalRoles = roles,
            totalPermissions = permissions,
            activeTheme = setting.theme
        )
    }
}
