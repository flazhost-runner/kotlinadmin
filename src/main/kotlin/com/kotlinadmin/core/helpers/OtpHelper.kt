package com.kotlinadmin.core.helpers

import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom

object OtpHelper {
    private const val OTP_UPPER_BOUND = 1_000_000
    private const val OTP_LENGTH = 6
    private const val MS_PER_MINUTE = 60_000L

    fun generate(): String {
        val code = SecureRandom().nextInt(OTP_UPPER_BOUND)
        return code.toString().padStart(OTP_LENGTH, '0')
    }

    fun hash(otp: String, rounds: Int = 10): String = BCrypt.hashpw(otp, BCrypt.gensalt(rounds))

    fun verify(otp: String, hashed: String): Boolean = BCrypt.checkpw(otp, hashed)

    fun expiry(minutesFromNow: Long = 10L): Long =
        System.currentTimeMillis() + minutesFromNow * MS_PER_MINUTE

    fun isExpired(expiryMs: Long): Boolean = System.currentTimeMillis() > expiryMs
}
