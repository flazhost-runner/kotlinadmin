package com.kotlinadmin.core.session

import kotlinx.serialization.Serializable

@Serializable
data class UserSession(
    val userId: String,
    val userName: String,
    val userEmail: String,
    val roles: List<String> = emptyList(),
    val flash: FlashMessage? = null,
    val errors: Map<String, String> = emptyMap(),
    val old: Map<String, String> = emptyMap(),
    val csrfToken: String? = null
)

@Serializable
data class FlashMessage(val key: String, val message: String)

fun UserSession.withFlash(key: String, message: String): UserSession =
    copy(flash = FlashMessage(key, message), errors = emptyMap(), old = emptyMap())

fun UserSession.withErrors(errors: Map<String, String>, old: Map<String, String>): UserSession =
    copy(errors = errors, old = old, flash = null)

fun UserSession.clearFlashAndErrors(): UserSession =
    copy(flash = null, errors = emptyMap(), old = emptyMap())
