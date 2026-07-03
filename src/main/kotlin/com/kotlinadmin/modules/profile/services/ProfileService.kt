package com.kotlinadmin.modules.profile.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.errors.ValidationError
import com.kotlinadmin.modules.access.models.UserEntity
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant

class ProfileService(private val bcryptRounds: Int = 12) : IProfileService {

    override suspend fun get(userId: String): UserEntity = dbQuery {
        UserEntity.findById(userId) ?: throw NotFoundError("User not found")
    }

    override suspend fun update(userId: String, dto: UpdateProfileDto): UserEntity = dbQuery {
        val user = UserEntity.findById(userId) ?: throw NotFoundError("User not found")

        dto.code?.let { user.code = it }
        dto.name?.let { user.name = it }
        dto.phone?.let { user.phone = it }
        dto.email?.let { user.email = it }
        dto.timezone?.let { user.timezone = it }
        dto.status?.let { user.status = it }

        if (!dto.password.isNullOrBlank()) {
            if (dto.password != dto.passwordConfirm) {
                throw ValidationError("Password confirmation does not match")
            }
            user.password = BCrypt.hashpw(dto.password, BCrypt.gensalt(bcryptRounds))
        }

        user.updatedAt = Instant.now()
        user
    }
}
