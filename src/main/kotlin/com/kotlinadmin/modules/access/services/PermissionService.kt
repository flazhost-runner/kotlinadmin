package com.kotlinadmin.modules.access.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.core.helpers.QueryHelper.ciLike
import com.kotlinadmin.core.helpers.paginateQuery
import com.kotlinadmin.core.routing.RouteRegistry
import com.kotlinadmin.modules.access.dto.PermissionDto
import com.kotlinadmin.modules.access.models.PermissionEntity
import com.kotlinadmin.modules.access.models.Permissions
import com.kotlinadmin.modules.access.models.RolesPermissions
import io.ktor.http.Parameters
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.Instant
import java.util.UUID

private const val DEFAULT_PAGE_SIZE = 10

class PermissionService : IPermissionService {

    override suspend fun index(params: Parameters): PaginateResult<PermissionEntity> = dbQuery {
        val page = params["q_page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = params["q_page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

        var query = Permissions.selectAll()
        params["q_name"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Permissions.name, it) } }
        params["q_method"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { Permissions.method eq it } }
        params["q_status"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { Permissions.status eq it } }
        params["q_guard"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { Permissions.guardName eq it } }
        params["q_desc"]?.takeIf { it.isNotBlank() }?.let {
            query = query.andWhere { ciLike(Permissions.description, it) }
        }

        paginateQuery(query, page, pageSize) { row -> PermissionEntity.wrapRow(row) }
    }

    override suspend fun store(dto: PermissionDto, actorId: String): PermissionEntity = dbQuery {
        val now = Instant.now()
        PermissionEntity.new(UUID.randomUUID().toString()) {
            name = dto.name
            guardName = dto.guardName
            method = dto.method
            status = dto.status
            description = dto.description
            createdBy = actorId
            updatedBy = actorId
            createdAt = now
            updatedAt = now
        }
    }

    override suspend fun edit(id: String): PermissionEntity = dbQuery {
        PermissionEntity.findById(id) ?: throw NotFoundError("Permission not found")
    }

    override suspend fun update(id: String, dto: PermissionDto, actorId: String): PermissionEntity = dbQuery {
        val perm = PermissionEntity.findById(id) ?: throw NotFoundError("Permission not found")
        perm.name = dto.name
        perm.guardName = dto.guardName
        perm.method = dto.method
        perm.status = dto.status
        perm.description = dto.description
        perm.updatedBy = actorId
        perm.updatedAt = Instant.now()
        perm
    }

    override suspend fun delete(id: String): Unit = dbQuery {
        val perm = PermissionEntity.findById(id) ?: throw NotFoundError("Permission not found")
        RolesPermissions.deleteWhere { RolesPermissions.permissionId eq id }
        perm.delete()
    }

    override suspend fun deleteSelected(ids: List<String>): Unit = dbQuery {
        RolesPermissions.deleteWhere { RolesPermissions.permissionId inList ids }
        Permissions.deleteWhere { Permissions.id inList ids.map { EntityID(it, Permissions) } }
    }

    override suspend fun syncFromRouteRegistry(): Int = dbQuery {
        val entries = RouteRegistry.all()
        var created = 0
        val now = Instant.now()

        for (entry in entries) {
            val guardName = if (entry.name.startsWith("api.")) "api" else "web"
            val existing = Permissions.selectAll()
                .where { (Permissions.name eq entry.name) and (Permissions.method eq entry.method) }
                .firstOrNull()

            if (existing == null) {
                PermissionEntity.new(UUID.randomUUID().toString()) {
                    name = entry.name
                    method = entry.method
                    this.guardName = guardName
                    status = "Active"
                    description = null
                    createdBy = "system"
                    updatedBy = "system"
                    createdAt = now
                    updatedAt = now
                }
                created++
            }
        }
        created
    }
}
