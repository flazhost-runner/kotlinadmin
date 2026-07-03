package com.kotlinadmin.modules.access.services

import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.modules.access.dto.PermissionDto
import com.kotlinadmin.modules.access.models.PermissionEntity
import io.ktor.http.Parameters

interface IPermissionService {
    suspend fun index(params: Parameters): PaginateResult<PermissionEntity>
    suspend fun store(dto: PermissionDto, actorId: String): PermissionEntity
    suspend fun edit(id: String): PermissionEntity
    suspend fun update(id: String, dto: PermissionDto, actorId: String): PermissionEntity
    suspend fun delete(id: String)
    suspend fun deleteSelected(ids: List<String>)
    suspend fun syncFromRouteRegistry(): Int
}
