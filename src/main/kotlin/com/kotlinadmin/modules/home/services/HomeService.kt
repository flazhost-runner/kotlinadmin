package com.kotlinadmin.modules.home.services

import com.kotlinadmin.modules.setting.models.SettingCache

class HomeService : IHomeService {

    override suspend fun getActiveTemplateHtml(): String {
        val setting = SettingCache.get()
        // Returns "default" to signal FreeMarker should render home/index.ftl (the native rich template).
        // A non-"default" value would mean serving cached downloaded HTML for a catalog template.
        // Full frontend template switcher (download + cache) is a roadmap feature.
        return if (setting.feTemplate == "agency-consulting-002-creative-agency" ||
            setting.feTemplate.isNullOrBlank()
        ) {
            "default"
        } else {
            setting.feTemplate
        }
    }
}
