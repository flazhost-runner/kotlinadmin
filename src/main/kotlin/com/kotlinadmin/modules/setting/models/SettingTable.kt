package com.kotlinadmin.modules.setting.models

import com.kotlinadmin.config.dbQuery
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.timestamp

// ── Table ─────────────────────────────────────────────────────────────────────

object Settings : IdTable<String>("settings") {
    override val id: Column<EntityID<String>> = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)

    val initial = varchar("initial", 255).nullable()
    val name = varchar("name", 255).nullable()
    val description = text("description").nullable()
    val icon = varchar("icon", 255).nullable()
    val favicon = varchar("favicon", 255).nullable()
    val logo = varchar("logo", 255).nullable()
    val loginImage = varchar("login_image", 255).nullable()
    val phone = varchar("phone", 255).nullable()
    val address = varchar("address", 255).nullable()
    val email = varchar("email", 255).nullable()
    val copyright = varchar("copyright", 255).nullable()
    val theme = varchar("theme", 20).default("Blue")
    val feTemplate = varchar("fe_template", 80).default("agency-consulting-002-creative-agency")
    val createdBy = varchar("created_by", 36).nullable()
    val updatedBy = varchar("updated_by", 36).nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
}

class SettingEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, SettingEntity>(Settings)

    var initial by Settings.initial
    var name by Settings.name
    var description by Settings.description
    var icon by Settings.icon
    var favicon by Settings.favicon
    var logo by Settings.logo
    var loginImage by Settings.loginImage
    var phone by Settings.phone
    var address by Settings.address
    var email by Settings.email
    var copyright by Settings.copyright
    var theme by Settings.theme
    var feTemplate by Settings.feTemplate
    var createdBy by Settings.createdBy
    var updatedBy by Settings.updatedBy
    var createdAt by Settings.createdAt
    var updatedAt by Settings.updatedAt

    fun toData() = SettingData(
        id = this.id.value,
        initial = initial,
        name = name,
        description = description,
        icon = icon,
        favicon = favicon,
        logo = logo,
        loginImage = loginImage,
        phone = phone,
        address = address,
        email = email,
        copyright = copyright,
        theme = theme,
        feTemplate = feTemplate
    )
}

// ── Data class ────────────────────────────────────────────────────────────────

data class SettingData(
    val id: String = "",
    val initial: String? = null,
    val name: String? = "KotlinAdmin",
    val description: String? = null,
    val icon: String? = null,
    val favicon: String? = null,
    val logo: String? = null,
    val loginImage: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val email: String? = null,
    val copyright: String? = null,
    val theme: String = "Blue",
    val feTemplate: String = "agency-consulting-002-creative-agency"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "initial" to initial,
        "name" to (name ?: "KotlinAdmin"),
        "description" to description,
        "icon" to icon,
        "favicon" to favicon,
        "logo" to logo,
        "loginImage" to loginImage,
        "login_image" to loginImage,
        "phone" to phone,
        "address" to address,
        "email" to email,
        "copyright" to copyright,
        "theme" to theme,
        "feTemplate" to feTemplate
    )
}

// ── Cache (TTL 60s) ───────────────────────────────────────────────────────────

object SettingCache {
    @Volatile private var cache: SettingData? = null

    @Volatile private var lastUpdated: Long = 0L
    private const val TTL_MS = 60_000L

    suspend fun get(): SettingData {
        val now = System.currentTimeMillis()
        if (cache == null || now - lastUpdated > TTL_MS) {
            cache = dbQuery {
                SettingEntity.all().firstOrNull()?.toData() ?: SettingData()
            }
            lastUpdated = System.currentTimeMillis()
        }
        return cache!!
    }

    fun invalidate() {
        cache = null
        lastUpdated = 0L
    }
}
