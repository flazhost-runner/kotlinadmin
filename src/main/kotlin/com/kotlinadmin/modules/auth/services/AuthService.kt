package com.kotlinadmin.modules.auth.services

import com.kotlinadmin.config.RedisManager
import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.core.errors.ConflictError
import com.kotlinadmin.core.errors.NotFoundError
import com.kotlinadmin.core.errors.UnauthorizedError
import com.kotlinadmin.core.errors.ValidationError
import com.kotlinadmin.core.helpers.OtpHelper
import com.kotlinadmin.modules.access.models.Roles
import com.kotlinadmin.modules.access.models.UserEntity
import com.kotlinadmin.modules.access.models.Users
import com.kotlinadmin.modules.access.models.UsersRoles
import com.kotlinadmin.modules.auth.dto.RegisterDto
import com.kotlinadmin.modules.auth.dto.ResetProcessDto
import com.kotlinadmin.modules.auth.dto.ResetRequestDto
import org.mindrot.jbcrypt.BCrypt
import java.time.Instant
import java.util.UUID

private const val RATE_LIMIT_WINDOW_SECONDS = 900L
private const val AUTH_RATE_LIMIT_MAX = 10
private const val OTP_RATE_LIMIT_MAX = 5
private const val USER_CODE_MODULO = 100_000
private const val MS_PER_MINUTE = 60 * 1000L

class AuthService(
    private val redis: RedisManager,
    private val bcryptRounds: Int = 10,
    private val otpExpiryMinutes: Long = 10L
) : IAuthService {

    // "localhost" tercakup: Ktor (termasuk test engine) melaporkan remote host
    // sebagai hostname loopback, bukan alamat IP.
    private fun isLoopback(ip: String) =
        ip == "127.0.0.1" || ip == "::1" || ip == "0:0:0:0:0:0:0:1" || ip == "localhost"

    // authLimiter: 10 req / 15 min / IP (loopback exempt)
    override suspend fun checkAuthRateLimit(ip: String) {
        if (isLoopback(ip)) return
        val key = "auth_rate:$ip"
        val count = redis.incrementRateLimit(key, RATE_LIMIT_WINDOW_SECONDS)
        if (count > AUTH_RATE_LIMIT_MAX) throw ValidationError("Too many requests. Please try again later.")
    }

    // otpLimiter: 5 req / 15 min / IP (loopback exempt)
    override suspend fun checkOtpRateLimit(ip: String) {
        if (isLoopback(ip)) return
        val key = "otp_proc_rate:$ip"
        val count = redis.incrementRateLimit(key, RATE_LIMIT_WINDOW_SECONDS)
        if (count > OTP_RATE_LIMIT_MAX) throw ValidationError("Too many OTP attempts. Please wait before trying again.")
    }

    override suspend fun login(email: String, password: String): UserEntity = dbQuery {
        val user = UserEntity.find { Users.email eq email }.firstOrNull()
            ?: throw UnauthorizedError("Wrong email or password.")
        if (user.status != "Active") throw UnauthorizedError("Account is inactive.")
        if (user.blocked) throw UnauthorizedError("Account is blocked.")
        if (!BCrypt.checkpw(password, user.password)) throw UnauthorizedError("Wrong email or password.")
        user
    }

    override suspend fun register(dto: RegisterDto): UserEntity {
        dto.passwordConfirm?.let {
            if (dto.password != it) throw ValidationError("Password confirmation does not match")
        }
        return dbQuery {
            if (UserEntity.find { Users.email eq dto.email }.firstOrNull() != null) {
                throw ConflictError("Email already exists.")
            }
            val now = Instant.now()
            UserEntity.new(UUID.randomUUID().toString()) {
                code = "U${System.currentTimeMillis() % USER_CODE_MODULO}"
                name = dto.name
                email = dto.email
                password = BCrypt.hashpw(dto.password, BCrypt.gensalt(bcryptRounds))
                status = "Active"
                timezone = "UTC"
                createdAt = now
                updatedAt = now
            }
        }
    }

    override suspend fun requestOtp(dto: ResetRequestDto) {
        val user = dbQuery {
            UserEntity.find { Users.email eq dto.email }.firstOrNull()
        } ?: return // Silently ignore — don't reveal email existence

        val otp = OtpHelper.generate()
        val otpHash = OtpHelper.hash(otp, bcryptRounds)
        val expires = System.currentTimeMillis() + (otpExpiryMinutes * MS_PER_MINUTE)

        dbQuery {
            user.passwordOtp = otpHash
            user.passwordOtpExpires = expires
            user.updatedAt = Instant.now()
        }
        // In production: send OTP via email
        println("[DEV] OTP for ${dto.email}: $otp")
    }

    override suspend fun processOtp(dto: ResetProcessDto) {
        dto.newPasswordConfirm?.let {
            if (dto.newPassword != it) throw ValidationError("Password confirmation does not match")
        }
        dbQuery {
            val user = UserEntity.find { Users.email eq dto.email }.firstOrNull()
                ?: throw ValidationError("Invalid reset request")
            val storedHash = user.passwordOtp ?: throw ValidationError("No OTP requested")
            val expires = user.passwordOtpExpires ?: throw ValidationError("No OTP requested")
            if (System.currentTimeMillis() > expires) throw ValidationError("OTP has expired.")
            if (!OtpHelper.verify(dto.otp, storedHash)) throw ValidationError("OTP is invalid.")

            user.password = BCrypt.hashpw(dto.newPassword, BCrypt.gensalt(bcryptRounds))
            user.passwordOtp = null
            user.passwordOtpExpires = null
            user.updatedAt = Instant.now()
        }
    }

    override suspend fun validateJwtUser(userId: String): UserEntity = dbQuery {
        val user = UserEntity.findById(userId) ?: throw UnauthorizedError("User not found")
        if (user.status != "Active") throw UnauthorizedError("Account is inactive")
        user
    }

    override suspend fun getUserRoleNames(userId: String): List<String> = dbQuery {
        (UsersRoles innerJoin Roles)
            .select(Roles.name)
            .where { UsersRoles.userId eq userId }
            .map { it[Roles.name] }
    }

    override suspend fun blacklistJwt(jti: String, ttlSeconds: Long) {
        redis.blacklistJwt(jti, ttlSeconds)
    }

    override suspend fun getById(userId: String): UserEntity = dbQuery {
        UserEntity.findById(userId) ?: throw NotFoundError("User not found")
    }
}
