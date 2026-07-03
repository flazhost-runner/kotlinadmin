package com.kotlinadmin.modules.setting.services

import com.kotlinadmin.config.dbQuery
import com.kotlinadmin.modules.setting.models.SettingCache
import com.kotlinadmin.modules.setting.models.SettingData
import com.kotlinadmin.modules.setting.models.SettingEntity
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.time.Instant
import java.util.UUID

class SettingService : ISettingService {

    override suspend fun get(): SettingData = SettingCache.get()

    override suspend fun update(dto: UpdateSettingDto, actorId: String): SettingData = dbQuery {
        val setting = SettingEntity.all().firstOrNull()
            ?: SettingEntity.new(UUID.randomUUID().toString()) {
                theme = "Blue"
                feTemplate = "agency-consulting-002-creative-agency"
                createdBy = actorId
                updatedBy = actorId
                val now = Instant.now()
                createdAt = now
                updatedAt = now
            }

        dto.initial?.let { setting.initial = it }
        dto.name?.let { setting.name = it }
        dto.description?.let { setting.description = sanitizeHtml(it) }
        dto.icon?.let { setting.icon = it }
        dto.logo?.let { setting.logo = it }
        dto.loginImage?.let { setting.loginImage = it }
        dto.phone?.let { setting.phone = it }
        dto.address?.let { setting.address = it }
        dto.email?.let { setting.email = it }
        dto.copyright?.let { setting.copyright = it }
        dto.theme?.let { setting.theme = it }
        dto.feTemplate?.let { setting.feTemplate = it }
        setting.updatedBy = actorId
        setting.updatedAt = Instant.now()

        SettingCache.invalidate()

        setting.toData()
    }

    override suspend fun invalidateCache() {
        SettingCache.invalidate()
    }

    override suspend fun previewTemplate(slug: String): String {
        // Stub: return placeholder HTML for the template preview
        return "<html><body><p>Template preview for: $slug</p></body></html>"
    }

    private fun sanitizeHtml(html: String): String =
        Jsoup.clean(html, Safelist.relaxed())
}
