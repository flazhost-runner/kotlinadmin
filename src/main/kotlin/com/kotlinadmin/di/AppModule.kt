package com.kotlinadmin.di

import com.kotlinadmin.config.AppConfig
import com.kotlinadmin.config.RedisManager
import com.kotlinadmin.modules.access.services.IPermissionService
import com.kotlinadmin.modules.access.services.IRoleService
import com.kotlinadmin.modules.access.services.IUserService
import com.kotlinadmin.modules.access.services.PermissionService
import com.kotlinadmin.modules.access.services.RoleService
import com.kotlinadmin.modules.access.services.UserService
import com.kotlinadmin.modules.auth.services.AuthService
import com.kotlinadmin.modules.auth.services.IAuthService
import com.kotlinadmin.modules.dashboard.services.DashboardService
import com.kotlinadmin.modules.dashboard.services.IDashboardService
import com.kotlinadmin.modules.home.services.FeCatalogService
import com.kotlinadmin.modules.home.services.HomeService
import com.kotlinadmin.modules.home.services.IFeCatalogService
import com.kotlinadmin.modules.home.services.IHomeService
import com.kotlinadmin.modules.media.services.IMediaService
import com.kotlinadmin.modules.media.services.MediaService
import com.kotlinadmin.modules.profile.services.IProfileService
import com.kotlinadmin.modules.profile.services.ProfileService
import com.kotlinadmin.modules.setting.services.ISettingService
import com.kotlinadmin.modules.setting.services.SettingService
import org.koin.dsl.module

fun appModule(config: AppConfig) = module {
    single { config }
    single { RedisManager }

    single<IAuthService> { AuthService(get<RedisManager>(), config.bcryptRounds, config.otpExpiryMinutes) }
    single<IUserService> { UserService() }
    single<IRoleService> { RoleService() }
    single<IPermissionService> { PermissionService() }
    single<IDashboardService> { DashboardService() }
    single<ISettingService> { SettingService() }
    single<IProfileService> { ProfileService() }
    single<IHomeService> { HomeService() }
    single<IFeCatalogService> { FeCatalogService(config.feTemplateRemote, config.feTemplateCacheDir) }
    single<IMediaService> { MediaService() }
}
