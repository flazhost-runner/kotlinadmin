package com.kotlinadmin.core.helpers

import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.LowerCase
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like

object QueryHelper {
    private val ALLOWED_PAGE_SIZES = listOf(10, 20, 50, 100)

    @JvmName("ciLikeNonNull")
    fun ciLike(column: Column<String>, value: String): Op<Boolean> {
        val lower = LowerCase(column)
        return lower like "%${value.lowercase()}%"
    }

    @JvmName("ciLikeNullable")
    fun ciLike(column: Column<String?>, value: String): Op<Boolean> {
        @Suppress("UNCHECKED_CAST")
        val lower = LowerCase(column as Column<String>)
        return lower like "%${value.lowercase()}%"
    }

    fun removeEmptyStrings(map: Map<String, String?>): Map<String, String> =
        map.filterValues { !it.isNullOrBlank() }.mapValues { it.value!! }

    fun parsePageSize(raw: String?, default: Int = 10): Int {
        val v = raw?.toIntOrNull() ?: default
        return if (v in ALLOWED_PAGE_SIZES) v else default
    }

    fun parsePage(raw: String?, default: Int = 1): Int {
        val v = raw?.toIntOrNull() ?: default
        return if (v < 1) 1 else v
    }
}
