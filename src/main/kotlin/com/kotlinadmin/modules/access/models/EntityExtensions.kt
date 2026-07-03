package com.kotlinadmin.modules.access.models

fun UserEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id.value,
    "code" to code,
    "name" to name,
    "phone" to phone,
    "email" to email,
    "status" to status,
    "picture" to picture,
    "timezone" to timezone,
    "blocked" to blocked,
    "created_at" to createdAt?.toString(),
    "updated_at" to updatedAt?.toString(),
    "roles" to try {
        roles.map { mapOf("id" to it.id.value, "name" to it.name) }
    } catch (_: Exception) {
        emptyList<Map<String, Any>>()
    }
)

fun RoleEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id.value,
    "name" to name,
    "status" to status,
    "description" to description,
    "created_at" to createdAt?.toString(),
    "updated_at" to updatedAt?.toString()
)

fun PermissionEntity.toMap(): Map<String, Any?> = mapOf(
    "id" to id.value,
    "name" to name,
    "guard_name" to guardName,
    "method" to method,
    "status" to status,
    "description" to description,
    "created_at" to createdAt?.toString(),
    "updated_at" to updatedAt?.toString()
)
