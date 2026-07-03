package com.kotlinadmin.modules.access.models

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

// ── Users ────────────────────────────────────────────────────────────────────

object Users : IdTable<String>("users") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)

    val code = varchar("code", 20).uniqueIndex("users_code_unique")
    val name = varchar("name", 50)
    val phone = varchar("phone", 15).nullable()
    val email = varchar("email", 255).uniqueIndex("users_email_unique")
    val emailVerifiedAt = timestamp("email_verified_at").nullable()
    val password = varchar("password", 255)
    val passwordOtp = varchar("password_otp", 255).nullable()
    val passwordOtpExpires = long("password_otp_expires").nullable()
    val status = varchar("status", 20).default("Active")
    val picture = varchar("picture", 255).nullable()
    val blocked = bool("blocked").default(false)
    val blockedReason = varchar("blocked_reason", 255).nullable()
    val timezone = varchar("timezone", 255).default("UTC")
    val createdBy = varchar("created_by", 36).nullable()
    val updatedBy = varchar("updated_by", 36).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

class UserEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserEntity>(Users)

    var code by Users.code
    var name by Users.name
    var phone by Users.phone
    var email by Users.email
    var emailVerifiedAt by Users.emailVerifiedAt
    var password by Users.password
    var passwordOtp by Users.passwordOtp
    var passwordOtpExpires by Users.passwordOtpExpires
    var status by Users.status
    var picture by Users.picture
    var blocked by Users.blocked
    var blockedReason by Users.blockedReason
    var timezone by Users.timezone
    var createdBy by Users.createdBy
    var updatedBy by Users.updatedBy
    var createdAt by Users.createdAt
    var updatedAt by Users.updatedAt

    var roles by RoleEntity via UsersRoles
}

// ── Roles ─────────────────────────────────────────────────────────────────────

object Roles : IdTable<String>("roles") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", 255).uniqueIndex("roles_name_unique")
    val guardName = varchar("guard_name", 20).default("web")
    val status = varchar("status", 20).default("Active")
    val description = varchar("desc", 255).nullable() // DB column: "desc"
    val createdBy = varchar("created_by", 36).nullable()
    val updatedBy = varchar("updated_by", 36).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

class RoleEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, RoleEntity>(Roles)

    var name by Roles.name
    var guardName by Roles.guardName
    var status by Roles.status
    var description by Roles.description
    var createdBy by Roles.createdBy
    var updatedBy by Roles.updatedBy
    var createdAt by Roles.createdAt
    var updatedAt by Roles.updatedAt

    var permissions by PermissionEntity via RolesPermissions
}

// ── Permissions ───────────────────────────────────────────────────────────────

object Permissions : IdTable<String>("permissions") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)

    val name = varchar("name", 255).index("permissions_name_idx")
    val guardName = varchar("guard_name", 20).default("web").index("permissions_guard_name_idx")
    val method = varchar("method", 255).nullable()
    val status = varchar("status", 20).default("Active")
    val description = varchar("desc", 255).nullable() // DB column: "desc"
    val createdBy = varchar("created_by", 36).nullable()
    val updatedBy = varchar("updated_by", 36).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

class PermissionEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PermissionEntity>(Permissions)

    var name by Permissions.name
    var guardName by Permissions.guardName
    var method by Permissions.method
    var status by Permissions.status
    var description by Permissions.description
    var createdBy by Permissions.createdBy
    var updatedBy by Permissions.updatedBy
    var createdAt by Permissions.createdAt
    var updatedAt by Permissions.updatedAt
}

// ── Join Tables ───────────────────────────────────────────────────────────────

object UsersRoles : Table("users_roles") {
    val userId = varchar("user_id", 36).references(Users.id)
    val roleId = varchar("role_id", 36).references(Roles.id)
    override val primaryKey = PrimaryKey(userId, roleId)
}

object RolesPermissions : Table("roles_permissions") {
    val roleId = varchar("role_id", 36).references(Roles.id)
    val permissionId = varchar("permission_id", 36).references(Permissions.id)
    override val primaryKey = PrimaryKey(roleId, permissionId)
}

// ── AllTables ─────────────────────────────────────────────────────────────────

object AllTables {
    val all = arrayOf(Users, Roles, Permissions, UsersRoles, RolesPermissions)
}
