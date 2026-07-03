package com.kotlinadmin.modules.access.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserCreateDto(
    val name: String,
    val email: String,
    val code: String = "",
    val password: String,
    val passwordConfirm: String? = null,
    val phone: String? = null,
    val status: String = "Active",
    val timezone: String = "UTC",
    val picture: String? = null,
    val roleIds: List<String> = emptyList()
)

@Serializable
data class UserUpdateDto(
    val name: String? = null,
    val email: String? = null,
    val code: String? = null,
    val password: String? = null,
    val passwordConfirm: String? = null,
    val phone: String? = null,
    val status: String? = null,
    val timezone: String? = null,
    val picture: String? = null,
    val roleIds: List<String>? = null
)

@Serializable
data class RoleDto(
    val name: String,
    val status: String = "Active",
    val description: String? = null
)

@Serializable
data class PermissionDto(
    val name: String,
    val method: String? = null,
    val status: String = "Active",
    val description: String? = null,
    val guardName: String = "web",
    val path: String? = null
)

@Serializable
data class DeleteSelectedDto(val selected: List<String>)

@Serializable
data class BulkPermissionDto(val selected: List<String>)
