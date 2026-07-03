package com.kotlinadmin.core.helpers

import org.jetbrains.exposed.sql.Query
import org.jetbrains.exposed.sql.ResultRow

private const val DEFAULT_PAGE_SIZE = 10

data class PaginateResult<T>(
    val items: List<T>,
    val paginateData: PaginateData
)

data class PaginateData(
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrev: Boolean
) {
    fun toMap(): Map<String, Any> = mapOf(
        "current_page" to page,
        "page_size" to pageSize,
        "total_data" to total,
        "total_page" to totalPages,
        "hasNext" to hasNext,
        "hasPrev" to hasPrev
    )
}

fun buildPaginateData(page: Int, pageSize: Int, total: Long): PaginateData {
    val safePageSize = if (pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
    val safePage = if (page <= 0) 1 else page
    val totalPages = ((total + safePageSize - 1) / safePageSize).toInt().coerceAtLeast(1)
    return PaginateData(
        page = safePage,
        pageSize = safePageSize,
        total = total,
        totalPages = totalPages,
        hasNext = safePage < totalPages,
        hasPrev = safePage > 1
    )
}

fun <T> paginate(items: List<T>, page: Int, pageSize: Int): PaginateResult<T> {
    val safePageSize = if (pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
    val total = items.size.toLong()
    val totalPages = if (total == 0L) 0 else ((total + safePageSize - 1) / safePageSize).toInt()
    val safePage = when {
        page < 1 -> 1
        totalPages > 0 && page > totalPages -> totalPages
        else -> page
    }
    val offset = (safePage - 1) * safePageSize
    val slice = items.drop(offset).take(safePageSize)
    return PaginateResult(
        items = slice,
        paginateData = PaginateData(
            page = safePage,
            pageSize = safePageSize,
            total = total,
            totalPages = totalPages,
            hasNext = safePage < totalPages,
            hasPrev = safePage > 1
        )
    )
}

// Used by services inside a transaction block: paginateQuery(query, page, size) { row -> ... }
fun <T> paginateQuery(query: Query, page: Int, pageSize: Int, mapper: (ResultRow) -> T): PaginateResult<T> {
    val safePageSize = if (pageSize <= 0) DEFAULT_PAGE_SIZE else pageSize
    val safePage = if (page <= 0) 1 else page
    val total = query.count()
    val offset = ((safePage - 1) * safePageSize).toLong()
    val items = query.limit(safePageSize, offset).map(mapper)
    val totalPages = ((total + safePageSize - 1) / safePageSize).toInt().coerceAtLeast(1)
    return PaginateResult(
        items = items,
        paginateData = PaginateData(
            page = safePage,
            pageSize = safePageSize,
            total = total,
            totalPages = totalPages,
            hasNext = safePage < totalPages,
            hasPrev = safePage > 1
        )
    )
}
