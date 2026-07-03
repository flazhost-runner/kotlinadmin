package com.kotlinadmin.modules.setting.services

import com.kotlinadmin.modules.setting.models.SettingData

data class UpdateSettingDto(
    val initial: String? = null,
    val name: String? = null,
    val tagline: String? = null,
    val description: String? = null,
    val keywords: String? = null,
    val icon: String? = null,
    val logo: String? = null,
    val loginImage: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val email: String? = null,
    val copyright: String? = null,
    val theme: String? = null,
    val primaryColor: String? = null,
    val sidebarColor: String? = null,
    val allowRegister: String? = null,
    val allowResetPassword: String? = null,
    val maintenanceMode: String? = null,
    val maintenanceMessage: String? = null,
    val timezone: String? = null,
    val feTemplate: String? = null
)

interface ISettingService {
    suspend fun get(): SettingData
    suspend fun update(dto: UpdateSettingDto, actorId: String): SettingData
    suspend fun invalidateCache()
    suspend fun previewTemplate(slug: String): String
}
