package com.kotlinadmin.modules.access.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.core.helpers.QueryHelper.ciLike
import com.kotlinadmin.core.helpers.paginateQuery
import com.kotlinadmin.modules.access.dto.RoleDto
import com.kotlinadmin.modules.access.models.*
import io.ktor.http.Parameters
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import java.time.Instant
import java.util.UUID

private const val DEFAULT_PAGE_SIZE = 10

class RoleService : IRoleService {

    override suspend fun index(params: Parameters): PaginateResult<RoleEntity> = dbQuery {
        val page = params["q_page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = params["q_page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

        var query = Roles.selectAll()
        params["q_name"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Roles.name, it) } }
        params["q_status"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { Roles.status eq it } }
        params["q_desc"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Roles.description, it) } }

        paginateQuery(query, page, pageSize) { row -> RoleEntity.wrapRow(row) }
    }

    override suspend fun store(dto: RoleDto, actorId: String): RoleEntity = dbQuery {
        if (RoleEntity.find { Roles.name eq dto.name }.firstOrNull() != null) {
            throw ConflictError("Role name already exists")
        }

        val now = Instant.now()
        RoleEntity.new(UUID.randomUUID().toString()) {
            name = dto.name
            status = dto.status
            description = dto.description
            createdBy = actorId
            updatedBy = actorId
            createdAt = now
            updatedAt = now
        }
    }

    override suspend fun edit(id: String): RoleEntity = dbQuery {
        RoleEntity.findById(id) ?: throw NotFoundError("Role not found")
    }

    override suspend fun update(id: String, dto: RoleDto, actorId: String): RoleEntity = dbQuery {
        val role = RoleEntity.findById(id) ?: throw NotFoundError("Role not found")
        val existing = RoleEntity.find { Roles.name eq dto.name }.firstOrNull()
        if (existing != null && existing.id.value != id) throw ConflictError("Role name already exists")

        role.name = dto.name
        role.status = dto.status
        role.description = dto.description
        role.updatedBy = actorId
        role.updatedAt = Instant.now()
        role
    }

    override suspend fun delete(id: String): Unit = dbQuery {
        val role = RoleEntity.findById(id) ?: throw NotFoundError("Role not found")
        RolesPermissions.deleteWhere { RolesPermissions.roleId eq id }
        UsersRoles.deleteWhere { UsersRoles.roleId eq id }
        role.delete()
    }

    override suspend fun deleteSelected(ids: List<String>): Unit = dbQuery {
        RolesPermissions.deleteWhere { RolesPermissions.roleId inList ids }
        UsersRoles.deleteWhere { UsersRoles.roleId inList ids }
        Roles.deleteWhere { Roles.id inList ids.map { EntityID(it, Roles) } }
    }

    override suspend fun getPermissions(
        roleId: String,
        params: Parameters
    ): PaginateResult<PermissionWithAssigned> = dbQuery {
        RoleEntity.findById(roleId) ?: throw NotFoundError("Role not found")

        val page = params["q_page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = params["q_page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

        val assignedIds = RolesPermissions
            .select(RolesPermissions.permissionId)
            .where { RolesPermissions.roleId eq roleId }
            .map { it[RolesPermissions.permissionId] }
            .toSet()

        var query = Permissions.selectAll()
        params["q_name"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Permissions.name, it) } }
        params["q_desc"]?.takeIf { it.isNotBlank() }?.let {
            query = query.andWhere { ciLike(Permissions.description, it) }
        }
        params["q_status"]?.takeIf { it.isNotBlank() }?.let { s ->
            when (s) {
                "Active" -> query = query.andWhere { Permissions.id inList assignedIds }
                "Inactive" -> query = query.andWhere { Permissions.id notInList assignedIds }
            }
        }

        // Sort: assigned first
        query = query.orderBy(
            Permissions.id to SortOrder.ASC
        )

        paginateQuery(query, page, pageSize) { row ->
            val perm = PermissionEntity.wrapRow(row)
            PermissionWithAssigned(perm, perm.id.value in assignedIds)
        }
    }

    override suspend fun assignPermission(roleId: String, permissionId: String): Unit = dbQuery {
        RoleEntity.findById(roleId) ?: throw NotFoundError("Role not found")
        val already = RolesPermissions.selectAll()
            .where { (RolesPermissions.roleId eq roleId) and (RolesPermissions.permissionId eq permissionId) }
            .count()
        if (already == 0L) {
            RolesPermissions.insert {
                it[RolesPermissions.roleId] = roleId
                it[RolesPermissions.permissionId] = permissionId
            }
        }
    }

    override suspend fun unassignPermission(roleId: String, permissionId: String): Unit = dbQuery {
        RolesPermissions.deleteWhere {
            (RolesPermissions.roleId eq roleId) and (RolesPermissions.permissionId eq permissionId)
        }
    }

    override suspend fun all(): List<RoleEntity> = dbQuery {
        RoleEntity.all().toList()
    }

    override suspend fun assignPermissions(roleId: String, permissionIds: List<String>): Unit = dbQuery {
        RoleEntity.findById(roleId) ?: throw NotFoundError("Role not found")
        val existing = RolesPermissions
            .select(RolesPermissions.permissionId)
            .where { RolesPermissions.roleId eq roleId }
            .map { it[RolesPermissions.permissionId] }
            .toSet()
        val toInsert = permissionIds.filter { it !in existing }
        if (toInsert.isNotEmpty()) {
            RolesPermissions.batchInsert(toInsert) { pid ->
                this[RolesPermissions.roleId] = roleId
                this[RolesPermissions.permissionId] = pid
            }
        }
    }

    override suspend fun unassignPermissions(roleId: String, permissionIds: List<String>): Unit = dbQuery {
        RolesPermissions.deleteWhere {
            (RolesPermissions.roleId eq roleId) and (RolesPermissions.permissionId inList permissionIds)
        }
    }

    override suspend fun getAssignedPermissionIds(roleId: String): Set<String> = dbQuery {
        RolesPermissions
            .select(RolesPermissions.permissionId)
            .where { RolesPermissions.roleId eq roleId }
            .map { it[RolesPermissions.permissionId] }
            .toSet()
    }
}
