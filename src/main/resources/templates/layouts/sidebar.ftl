<#ftl output_format="HTML">
<div class="flex flex-col h-full">
  <div class="px-4 py-5 border-b border-white/20">
    <a href="/admin/v1/dashboard" class="flex items-center gap-3 text-white no-underline">
      <img src="${setting.icon!""}" alt="logo" width="36" height="36" class="rounded"
           onerror="this.style.display='none'; this.nextElementSibling.style.display='inline-flex'">
      <span style="display:none" class="w-9 h-9 bg-white/20 rounded items-center justify-center">
        <i class="fas fa-chart-line text-white"></i>
      </span>
      <span class="font-semibold text-lg">${setting.name!"KotlinAdmin"}</span>
    </a>
  </div>

  <nav class="flex-1 py-4 overflow-y-auto">
    <ul class="list-none p-0 m-0 space-y-0.5">
      <#-- Dashboard -->
      <li>
        <a href="/admin/v1/dashboard"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/admin/v1/dashboard')>active</#if>">
          <i class="fas fa-tachometer-alt w-5 text-center"></i>
          <span>Dashboard</span>
        </a>
      </li>

      <#-- UI Components -->
      <#if !(sidebarAccess??) || (sidebarAccess.components)!true>
      <li>
        <a href="/admin/v1/components"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/admin/v1/components')>active</#if>">
          <i class="fas fa-cubes w-5 text-center"></i>
          <span>UI Components</span>
        </a>
      </li>
      </#if>

      <#-- Maintenance Section -->
      <#assign showMaintenance = (!(sidebarAccess??) || (sidebarAccess.permission)!true || (sidebarAccess.role)!true || (sidebarAccess.user)!true || (sidebarAccess.setting)!true)>
      <#if showMaintenance>
      <li class="px-4 pt-4 pb-1">
        <span class="text-white/50 text-xs font-semibold uppercase tracking-wider">Maintenance</span>
      </li>
      </#if>

      <#if !(sidebarAccess??) || (sidebarAccess.permission)!true>
      <li>
        <a href="/admin/v1/access/permission"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/access/permission')>active</#if>">
          <i class="fas fa-key w-5 text-center"></i>
          <span>Permission</span>
        </a>
      </li>
      </#if>

      <#if !(sidebarAccess??) || (sidebarAccess.role)!true>
      <li>
        <a href="/admin/v1/access/role"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/access/role')>active</#if>">
          <i class="fas fa-user-shield w-5 text-center"></i>
          <span>Role</span>
        </a>
      </li>
      </#if>

      <#if !(sidebarAccess??) || (sidebarAccess.user)!true>
      <li>
        <a href="/admin/v1/access/user"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/access/user')>active</#if>">
          <i class="fas fa-users w-5 text-center"></i>
          <span>User</span>
        </a>
      </li>
      </#if>

      <#if !(sidebarAccess??) || (sidebarAccess.setting)!true>
      <li>
        <a href="/admin/v1/setting"
           class="nav-link-tw <#if currentPath?? && currentPath?contains('/admin/v1/setting')>active</#if>">
          <i class="fas fa-cog w-5 text-center"></i>
          <span>Setting</span>
        </a>
      </li>
      </#if>
    </ul>
  </nav>

  <div class="px-4 py-3 border-t border-white/20">
    <p class="text-white/50 text-xs text-center">${setting.copyright!"&copy; KotlinAdmin"}</p>
  </div>
</div>
