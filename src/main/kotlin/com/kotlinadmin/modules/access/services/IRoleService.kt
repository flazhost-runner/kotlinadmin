package com.kotlinadmin.modules.access.services

import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.modules.access.dto.RoleDto
import com.kotlinadmin.modules.access.models.PermissionEntity
import com.kotlinadmin.modules.access.models.RoleEntity
import io.ktor.http.Parameters

data class PermissionWithAssigned(val permission: PermissionEntity, val assigned: Boolean) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to permission.id.value,
        "name" to permission.name,
        "guardName" to permission.guardName,
        "method" to permission.method,
        "status" to permission.status,
        "description" to permission.description,
        "assigned" to assigned
    )
}

interface IRoleService {
    suspend fun index(params: Parameters): PaginateResult<RoleEntity>
    suspend fun all(): List<RoleEntity>
    suspend fun store(dto: RoleDto, actorId: String): RoleEntity
    suspend fun edit(id: String): RoleEntity
    suspend fun update(id: String, dto: RoleDto, actorId: String): RoleEntity
    suspend fun delete(id: String)
    suspend fun deleteSelected(ids: List<String>)
    suspend fun getPermissions(roleId: String, params: Parameters): PaginateResult<PermissionWithAssigned>
    suspend fun assignPermission(roleId: String, permissionId: String)
    suspend fun unassignPermission(roleId: String, permissionId: String)
    suspend fun assignPermissions(roleId: String, permissionIds: List<String>)
    suspend fun unassignPermissions(roleId: String, permissionIds: List<String>)
    suspend fun getAssignedPermissionIds(roleId: String): Set<String>
}
