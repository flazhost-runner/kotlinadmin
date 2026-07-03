package com.kotlinadmin.modules.auth.services

import com.kotlinadmin.modules.access.models.UserEntity
import com.kotlinadmin.modules.auth.dto.RegisterDto
import com.kotlinadmin.modules.auth.dto.ResetProcessDto
import com.kotlinadmin.modules.auth.dto.ResetRequestDto

interface IAuthService {
    suspend fun checkAuthRateLimit(ip: String)
    suspend fun checkOtpRateLimit(ip: String)
    suspend fun login(email: String, password: String): UserEntity
    suspend fun register(dto: RegisterDto): UserEntity
    suspend fun requestOtp(dto: ResetRequestDto)
    suspend fun processOtp(dto: ResetProcessDto)
    suspend fun validateJwtUser(userId: String): UserEntity
    suspend fun getUserRoleNames(userId: String): List<String>
    suspend fun blacklistJwt(jti: String, ttlSeconds: Long)
    suspend fun getById(userId: String): UserEntity
}
