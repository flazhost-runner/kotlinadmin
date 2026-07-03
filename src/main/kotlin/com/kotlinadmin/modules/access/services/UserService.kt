package com.kotlinadmin.modules.access.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.errors.ValidationError
import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.core.helpers.QueryHelper.ciLike
import com.kotlinadmin.core.helpers.paginateQuery
import com.kotlinadmin.modules.access.dto.UserCreateDto
import com.kotlinadmin.modules.access.dto.UserUpdateDto
import com.kotlinadmin.modules.access.models.*
import io.ktor.http.Parameters
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

private const val DEFAULT_PAGE_SIZE = 10

class UserService(private val bcryptRounds: Int = 12) : IUserService {

    override suspend fun index(params: Parameters): PaginateResult<UserEntity> = dbQuery {
        val page = params["q_page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = params["q_page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
        val qRole = params["q_role"]

        var query = Users.selectAll()

        params["q_code"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Users.code, it) } }
        params["q_name"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Users.name, it) } }
        params["q_phone"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Users.phone, it) } }
        params["q_email"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { ciLike(Users.email, it) } }
        params["q_status"]?.takeIf { it.isNotBlank() }?.let { query = query.andWhere { Users.status eq it } }

        if (!qRole.isNullOrBlank()) {
            val roleIds = UsersRoles
                .innerJoin(Roles, { UsersRoles.roleId }, { Roles.id })
                .select(UsersRoles.userId)
                .where { Roles.name eq qRole }
                .map { it[UsersRoles.userId] }
            query = query.andWhere { Users.id inList roleIds }
        }

        paginateQuery(query, page, pageSize) { row -> UserEntity.wrapRow(row) }
    }

    override suspend fun store(dto: UserCreateDto, actorId: String): UserEntity {
        if (dto.password != dto.passwordConfirm) throw ValidationError("Password confirmation does not match")

        return dbQuery {
            if (UserEntity.find { Users.code eq dto.code }.firstOrNull() != null) {
                throw ConflictError("Code already exists")
            }
            if (UserEntity.find { Users.email eq dto.email }.firstOrNull() != null) {
                throw ConflictError("Email already exists.")
            }

            val now = Instant.now()
            val user = UserEntity.new(UUID.randomUUID().toString()) {
                code = dto.code
                name = dto.name
                email = dto.email
                phone = dto.phone
                password = BCrypt.hashpw(dto.password, BCrypt.gensalt(bcryptRounds))
                status = dto.status
                timezone = dto.timezone
                createdBy = actorId
                updatedBy = actorId
                createdAt = now
                updatedAt = now
            }

            if (dto.roleIds.isNotEmpty()) {
                UsersRoles.batchInsert(dto.roleIds) { roleId ->
                    this[UsersRoles.userId] = user.id.value
                    this[UsersRoles.roleId] = roleId
                }
            }
            user
        }
    }

    override suspend fun edit(id: String): UserEntity = dbQuery {
        UserEntity.findById(id) ?: throw NotFoundError("User not found")
    }

    override suspend fun update(id: String, dto: UserUpdateDto, actorId: String): UserEntity = dbQuery {
        val user = UserEntity.findById(id) ?: throw NotFoundError("User not found")

        dto.code?.let { c ->
            val existing = UserEntity.find { Users.code eq c }.firstOrNull()
            if (existing != null && existing.id.value != id) throw ConflictError("Code already exists")
            user.code = c
        }
        dto.email?.let { e ->
            val existing = UserEntity.find { Users.email eq e }.firstOrNull()
            if (existing != null && existing.id.value != id) throw ConflictError("Email already exists.")
            user.email = e
        }
        dto.name?.let { user.name = it }
        dto.phone?.let { user.phone = it }
        dto.status?.let { user.status = it }
        dto.timezone?.let { user.timezone = it }

        if (!dto.password.isNullOrBlank()) {
            if (dto.password != dto.passwordConfirm) throw ValidationError("Password confirmation does not match")
            user.password = BCrypt.hashpw(dto.password, BCrypt.gensalt(bcryptRounds))
        }

        user.updatedBy = actorId
        user.updatedAt = Instant.now()

        dto.roleIds?.let { roleIds ->
            UsersRoles.deleteWhere { UsersRoles.userId eq id }
            if (roleIds.isNotEmpty()) {
                UsersRoles.batchInsert(roleIds) { roleId ->
                    this[UsersRoles.userId] = id
                    this[UsersRoles.roleId] = roleId
                }
            }
        }
        user
    }

    override suspend fun delete(id: String): Unit = dbQuery {
        val user = UserEntity.findById(id) ?: throw NotFoundError("User not found")
        UsersRoles.deleteWhere { UsersRoles.userId eq id }
        user.delete()
    }

    override suspend fun deleteSelected(ids: List<String>): Unit = dbQuery {
        UsersRoles.deleteWhere { UsersRoles.userId inList ids }
        Users.deleteWhere { Users.id inList ids.map { EntityID(it, Users) } }
    }

    override suspend fun getUserRoles(userId: String): List<String> = dbQuery {
        (UsersRoles innerJoin Roles)
            .select(Roles.name)
            .where { UsersRoles.userId eq userId }
            .map { it[Roles.name] }
    }
}
