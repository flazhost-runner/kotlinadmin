package com.kotlinadmin.core.errors

import io.ktor.http.HttpStatusCode

sealed class AppException(msg: String, val statusCode: HttpStatusCode) : RuntimeException(msg)

class NotFoundError(msg: String = "Not Found") : AppException(msg, HttpStatusCode.NotFound)
class ConflictError(msg: String = "Conflict") : AppException(msg, HttpStatusCode.Conflict)
class ValidationError(
    msg: String = "Validation Failed",
    val fieldErrors: Map<String, String> = emptyMap()
) : AppException(msg, HttpStatusCode.UnprocessableEntity)
class UnauthorizedError(msg: String = "Unauthorized") : AppException(msg, HttpStatusCode.Unauthorized)
class ForbiddenError(msg: String = "Forbidden") : AppException(msg, HttpStatusCode.Forbidden)

data class ThemeData(
    val name: String,
    val primary: String,
    val secondary: String,
    val light: String,
    val dark: String
) {
    fun toMap(): Map<String, String> = mapOf(
        "name" to name,
        "primary" to primary,
        "secondary" to secondary,
        "light" to light,
        "dark" to dark
    )
}

val THEMES: List<ThemeData> = listOf(
    ThemeData("Blue", "#3B82F6", "#1D4ED8", "#EFF6FF", "#1E3A5F"),
    ThemeData("Purple", "#8B5CF6", "#6D28D9", "#F5F3FF", "#4C1D95"),
    ThemeData("Green", "#10B981", "#065F46", "#ECFDF5", "#064E3B"),
    ThemeData("Orange", "#F59E0B", "#B45309", "#FFFBEB", "#78350F"),
    ThemeData("Red", "#EF4444", "#B91C1C", "#FEF2F2", "#7F1D1D")
)

fun getTheme(name: String): ThemeData =
    THEMES.find { it.name.equals(name, ignoreCase = true) } ?: THEMES.first()
