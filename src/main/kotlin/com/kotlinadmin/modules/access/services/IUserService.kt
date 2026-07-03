package com.kotlinadmin.modules.access.services

import com.kotlinadmin.core.helpers.PaginateResult
import com.kotlinadmin.modules.access.dto.UserCreateDto
import com.kotlinadmin.modules.access.dto.UserUpdateDto
import com.kotlinadmin.modules.access.models.UserEntity
import io.ktor.http.Parameters

interface IUserService {
    suspend fun index(params: Parameters): PaginateResult<UserEntity>
    suspend fun store(dto: UserCreateDto, actorId: String): UserEntity
    suspend fun edit(id: String): UserEntity
    suspend fun update(id: String, dto: UserUpdateDto, actorId: String): UserEntity
    suspend fun delete(id: String)
    suspend fun deleteSelected(ids: List<String>)
    suspend fun getUserRoles(userId: String): List<String>
}
