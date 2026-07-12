package com.kotlinadmin.modules.profile.services

import com.kotlinadmin.modules.access.models.UserEntity

data class UpdateProfileDto(
    val code: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val timezone: String? = null,
    val status: String? = null,
    val password: String? = null,
    val passwordConfirm: String? = null,
    /** Storage key of a newly-uploaded avatar (e.g. `user/<id>.png`); null = keep existing. */
    val picture: String? = null
)

interface IProfileService {
    suspend fun get(userId: String): UserEntity
    suspend fun update(userId: String, dto: UpdateProfileDto): UserEntity
}
