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

// 9 palet kanonik — hex persis `@flazhost-nodeadmin/core` THEMES
// (urutan kunci NodeAdmin: Blue default dulu, lalu alfabetis).
val THEMES: List<ThemeData> = listOf(
    ThemeData("Blue", "#3B82F6", "#60A5FA", "#DBEAFE", "#1E40AF"),
    ThemeData("Black", "#374151", "#4B5563", "#6B7280", "#1F2937"),
    ThemeData("Brown", "#A16207", "#D97706", "#FEF3C7", "#78350F"),
    ThemeData("Green", "#10B981", "#34D399", "#D1FAE5", "#047857"),
    ThemeData("Grey", "#6B7280", "#9CA3AF", "#E5E7EB", "#374151"),
    ThemeData("Orange", "#F59E0B", "#FBBF24", "#FEF3C7", "#D97706"),
    ThemeData("Purple", "#8B5CF6", "#A78BFA", "#F3E8FF", "#6D28D9"),
    ThemeData("Red", "#EF4444", "#F87171", "#FECACA", "#B91C1C"),
    ThemeData("Yellow", "#F59E0B", "#FCD34D", "#FEF3C7", "#D97706")
)

fun getTheme(name: String): ThemeData =
    THEMES.find { it.name.equals(name, ignoreCase = true) } ?: THEMES.first()
