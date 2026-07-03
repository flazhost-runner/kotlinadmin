package com.kotlinadmin.modules.dashboard.services

data class DashboardStats(
    val totalUsers: Long,
    val totalRoles: Long,
    val totalPermissions: Long,
    val activeTheme: String
)

interface IDashboardService {
    suspend fun getStats(): DashboardStats
}
