package com.kotlinadmin.modules.home.services

import com.kotlinadmin.modules.setting.models.SettingCache

class HomeService : IHomeService {

    override suspend fun activeSlug(): String {
        val slug = SettingCache.get().feTemplate
        return if (slug.isBlank() || !isValidFeSlug(slug)) DEFAULT_FE_TEMPLATE else slug
    }

    override suspend fun landing(): Map<String, Any?> {
        val s = SettingCache.get()
        return mapOf(
            "app_name" to (s.name?.ifBlank { null } ?: "KotlinAdmin"),
            "description" to (s.description ?: ""),
            "logo" to (s.logo ?: ""),
            "email" to (s.email ?: ""),
            "phone" to (s.phone ?: ""),
            "address" to (s.address ?: ""),
            "copyright" to (s.copyright?.ifBlank { null } ?: "© KotlinAdmin")
        )
    }
}
