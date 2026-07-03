package com.kotlinadmin.modules.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginDto(val email: String, val password: String)

@Serializable
data class RegisterDto(
    val name: String,
    val email: String,
    val password: String,
    val passwordConfirm: String? = null
)

@Serializable
data class ResetRequestDto(val email: String)

@Serializable
data class ResetProcessDto(
    val email: String,
    val otp: String,
    val newPassword: String,
    val newPasswordConfirm: String? = null
)
