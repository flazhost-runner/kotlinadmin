package com.kotlinadmin.modules.home.services

interface IHomeService {
    /** Slug frontend template aktif dari Setting (fallback [DEFAULT_FE_TEMPLATE]). */
    suspend fun activeSlug(): String

    /** View-model landing (binding Setting + fallback aman — paritas GoAdmin HomeService.Landing). */
    suspend fun landing(): Map<String, Any?>
}
