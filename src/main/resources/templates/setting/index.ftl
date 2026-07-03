<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Settings</title>
</head>
<body class="bg-gray-100">
<div class="flex h-screen overflow-hidden">
  <aside class="hidden md:block w-64 sidebar-gradient fixed h-full overflow-y-auto z-20">
    <#include "/layouts/sidebar.ftl">
  </aside>
  <div class="flex-1 md:ml-64 flex flex-col min-h-screen overflow-y-auto">
    <header class="tw-card sticky top-0 z-10 rounded-none border-x-0 border-t-0">
      <#include "/layouts/topbar.ftl">
    </header>
    <main class="flex-1 p-6">
      <form method="POST" action="/admin/v1/setting/update?_method=PUT&_csrf=${_csrf}" enctype="multipart/form-data">

        <!-- General -->
        <div class="tw-card p-6 mb-6">
          <h2 class="text-lg font-semibold mb-4" style="color:var(--primary)">Setting Form</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="form-label">[name] App Name</label>
              <input type="text" name="name" value="${setting.name!""}" class="form-control" required>
            </div>
            <div>
              <label class="form-label">[tagline] Tagline</label>
              <input type="text" name="tagline" value="${setting.tagline!""}" class="form-control">
            </div>
            <div>
              <label class="form-label">[description] Description</label>
              <textarea name="description" rows="3" class="trumbowyg-editor form-control">${setting.description!""}</textarea>
            </div>
            <div>
              <label class="form-label">[keywords] Keywords</label>
              <input type="text" name="keywords" value="${setting.keywords!""}" class="form-control">
            </div>
            <div>
              <label class="form-label">[email] Contact Email</label>
              <input type="email" name="email" value="${setting.email!""}" class="form-control">
            </div>
            <div>
              <label class="form-label">[phone] Phone</label>
              <input type="text" name="phone" value="${setting.phone!""}" class="form-control">
            </div>
            <div>
              <label class="form-label">[address] Address</label>
              <textarea name="address" rows="2" class="form-control">${setting.address!""}</textarea>
            </div>
            <div>
              <label class="form-label">[timezone] Default Timezone</label>
              <select name="timezone" class="form-control select2">
                <#assign tzList = ["UTC","Asia/Jakarta","Asia/Makassar","Asia/Jayapura","Asia/Singapore","America/New_York","America/Los_Angeles","Europe/London","Europe/Paris"]>
                <#list tzList as tz>
                  <option value="${tz}" <#if (setting.timezone!"UTC") == tz>selected</#if>>${tz}</option>
                </#list>
              </select>
            </div>
          </div>
        </div>

        <!-- Admin Theme -->
        <div class="tw-card p-6 mb-6">
          <div class="flex items-center gap-2 mb-1">
            <i class="fas fa-palette" style="color:var(--primary)"></i>
            <h2 class="text-lg font-semibold" style="color:var(--primary)">Admin Theme</h2>
          </div>
          <p class="text-sm text-gray-500 mb-4">Choose a theme — admin appearance will update after saving.</p>
          <div class="d-flex gap-3 flex-wrap">
            <#list themes as t>
            <label class="cursor-pointer block">
              <input type="radio" name="theme" value="${t.name}" class="sr-only theme-radio"
                     <#if (setting.theme!"Blue") == t.name>checked</#if>
                     onchange="applyThemePreview(this)">
              <div class="theme-swatch rounded-xl overflow-hidden border-2 transition"
                   style="width:90px;cursor:pointer;border-color:<#if (setting.theme!"Blue") == t.name>var(--primary)<#else>transparent</#if>;box-shadow:0 4px 10px rgba(0,0,0,.08)">
                <div style="height:40px;display:flex">
                  <div style="flex:1;background:${t.dark}"></div>
                  <div style="flex:1;background:${t.primary}"></div>
                  <div style="flex:1;background:${t.secondary}"></div>
                  <div style="flex:1;background:${t.light}"></div>
                </div>
                <div class="bg-white py-1 px-2 d-flex align-items-center justify-content-between">
                  <span class="text-xs fw-semibold text-gray-700">${t.name}</span>
                  <i class="fas fa-check-circle check-icon<#if (setting.theme!"Blue") != t.name> hidden</#if>" style="color:${t.primary}"></i>
                </div>
              </div>
            </label>
            </#list>
          </div>
        </div>

        <!-- Frontend Template -->
        <input type="hidden" id="fe_template_input" name="fe_template" value="${setting.fe_template!""}">
        <div class="tw-card p-6 mb-6">
          <div class="flex items-center gap-2 mb-1">
            <i class="fas fa-window-maximize" style="color:var(--primary)"></i>
            <h2 class="text-lg font-semibold" style="color:var(--primary)">Frontend Template</h2>
          </div>
          <p class="text-sm text-gray-500 mb-3">Choose a frontend landing page template. It will be downloaded and applied on Save.</p>
          <form id="fe_search" method="GET" action="/admin/v1/setting"></form>
          <div class="d-flex flex-wrap gap-2 mb-3">
            <input form="fe_search" type="text" name="q_name" placeholder="Search templates…" class="form-control" style="max-width:200px">
            <button form="fe_search" type="submit" class="btn btn-success btn-sm"><i class="fas fa-search me-1"></i> Search</button>
            <a href="/admin/v1/setting" class="btn btn-danger btn-sm"><i class="fas fa-times me-1"></i> Reset</a>
          </div>
          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-3">
            <#assign feOptions = ["agency-consulting-002-creative-agency","portfolio-001-minimal","corporate-003-professional","landing-004-startup","saas-005-saas-landing","ecommerce-001-online-shop"]>
            <#list feOptions as fe>
            <div class="fe-card" data-slug="${fe}">
              <div class="fe-swatch rounded-xl overflow-hidden border-2 <#if (setting.fe_template!"") == fe>border-gray-900<#else>border-gray-300</#if>" style="box-shadow:0 2px 8px rgba(0,0,0,.12)">
                <div class="fe-thumb fe-preview-trigger bg-gray-100 d-flex align-items-center justify-content-center" style="height:80px">
                  <i class="fas fa-image fa-2x text-gray-300"></i>
                </div>
                <div class="bg-white py-2 px-3">
                  <div class="d-flex align-items-center justify-content-between">
                    <span class="text-xs fw-semibold text-gray-800 text-truncate">${fe}</span>
                    <i class="fas fa-check-circle fe-check<#if (setting.fe_template!"") != fe> hidden</#if>" style="color:var(--primary)"></i>
                  </div>
                  <button type="button" class="fe-select btn btn-sm w-100 mt-2 fw-bold <#if (setting.fe_template!"") == fe>btn-primary-tw<#else>btn-outline-dark</#if>" style="font-size:11px">
                    <#if (setting.fe_template!"") == fe>
                    <i class="fas fa-check me-1"></i> TERPILIH
                    <#else>
                    <i class="fas fa-hand-pointer me-1"></i> PILIH
                    </#if>
                  </button>
                </div>
              </div>
            </div>
            </#list>
          </div>
        </div>

        <!-- Logo / Images -->
        <div class="tw-card p-6 mb-6">
          <h2 class="text-lg font-semibold mb-4" style="color:var(--primary)">Branding</h2>
          <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div>
              <label class="form-label">[logo] Logo</label>
              <img id="logo-preview" src="${setting.logo!""}" alt="logo" width="80" height="80"
                   class="rounded mb-2 object-contain border border-gray-200 bg-white" style="display:block">
              <input type="file" name="logo" accept="image/*" class="form-control"
                     onchange="previewImage(this, 'logo-preview')">
            </div>
            <div>
              <label class="form-label">[icon] App Icon</label>
              <img id="icon-preview" src="${setting.icon!""}" alt="icon" width="40" height="40"
                   class="rounded mb-2 object-contain border border-gray-200 bg-white" style="display:block">
              <input type="file" name="icon" accept="image/*" class="form-control"
                     onchange="previewImage(this, 'icon-preview')">
            </div>
            <div>
              <label class="form-label">[favicon] Favicon</label>
              <#if setting.favicon?has_content>
              <img id="favicon-preview" src="${setting.favicon}" alt="favicon" width="32" height="32"
                   class="rounded mb-2 object-contain border border-gray-200 bg-white" style="display:block">
              <#else>
              <img id="favicon-preview" src="" alt="favicon" width="32" height="32"
                   class="rounded mb-2 object-contain border border-gray-200 bg-white" style="display:none">
              </#if>
              <input type="file" name="favicon" accept="image/*" class="form-control"
                     onchange="previewImage(this, 'favicon-preview')">
            </div>
            <div>
              <label class="form-label">[login_image] Login Image</label>
              <img id="login-img-preview" src="${setting.loginImage!""}" alt="login image" width="120" height="80"
                   class="rounded mb-2 object-cover border border-gray-200" style="display:block">
              <input type="file" name="login_image" accept="image/*" class="form-control"
                     onchange="previewImage(this, 'login-img-preview')">
            </div>
          </div>
        </div>

        <!-- Auth -->
        <div class="tw-card p-6 mb-6">
          <h2 class="text-lg font-semibold mb-4" style="color:var(--primary)">Auth & Features</h2>
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="form-label">[allow_register] Allow Registration</label>
              <select name="allowRegister" class="form-control">
                <option value="1" <#if (setting.allowRegister!"1") == "1">selected</#if>>Yes</option>
                <option value="0" <#if (setting.allowRegister!"") == "0">selected</#if>>No</option>
              </select>
            </div>
            <div>
              <label class="form-label">[allow_reset_password] Allow Password Reset</label>
              <select name="allowResetPassword" class="form-control">
                <option value="1" <#if (setting.allowResetPassword!"1") == "1">selected</#if>>Yes</option>
                <option value="0" <#if (setting.allowResetPassword!"") == "0">selected</#if>>No</option>
              </select>
            </div>
            <div>
              <label class="form-label">[maintenance_mode] Maintenance Mode</label>
              <select name="maintenanceMode" class="form-control">
                <option value="0" <#if (setting.maintenanceMode!"0") == "0">selected</#if>>Off</option>
                <option value="1" <#if (setting.maintenanceMode!"") == "1">selected</#if>>On</option>
              </select>
            </div>
            <div>
              <label class="form-label">[maintenance_message] Maintenance Message</label>
              <input type="text" name="maintenanceMessage" value="${setting.maintenanceMessage!""}" class="form-control">
            </div>
          </div>
        </div>

        <div class="flex gap-2 pb-4">
          <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Save Settings</button>
        </div>
      </form>
    </main>
  </div>
</div>


<#include "/layouts/foot.ftl">
<script>
// Theme live preview
var THEME_MAP = {};
<#list themes as t>
THEME_MAP['${t.name}'] = { primary: '${t.primary}', secondary: '${t.secondary}', light: '${t.light}', dark: '${t.dark}' };
</#list>

function applyThemePreview(radio) {
  var t = THEME_MAP[radio.value];
  if (!t) return;
  document.documentElement.style.setProperty('--primary', t.primary);
  document.documentElement.style.setProperty('--secondary', t.secondary);
  document.documentElement.style.setProperty('--theme-light', t.light);
  document.documentElement.style.setProperty('--theme-dark', t.dark);
  document.querySelectorAll('.theme-swatch').forEach(function(s) {
    s.style.borderColor = 'transparent';
  });
  document.querySelectorAll('.check-icon').forEach(function(c) { c.classList.add('hidden'); });
  var swatch = radio.nextElementSibling;
  if (swatch) swatch.style.borderColor = 'var(--primary)';
  var icon = radio.closest('label') && radio.closest('label').querySelector('.check-icon');
  if (icon) icon.classList.remove('hidden');
}

// FE template selection (inline cards)
var feInput = document.getElementById('fe_template_input');
document.querySelectorAll('.fe-select').forEach(function(btn) {
  btn.addEventListener('click', function() {
    var slug = this.closest('.fe-card').getAttribute('data-slug');
    if (feInput) feInput.value = slug;
    document.querySelectorAll('.fe-card').forEach(function(c) {
      var isA = c.getAttribute('data-slug') === slug;
      var sw = c.querySelector('.fe-swatch');
      var ch = c.querySelector('.fe-check');
      var b  = c.querySelector('.fe-select');
      if (sw) { sw.classList.toggle('border-gray-900', isA); sw.classList.toggle('border-gray-300', !isA); }
      if (ch) ch.classList.toggle('hidden', !isA);
      if (b) {
        b.classList.toggle('btn-primary-tw', isA);
        b.classList.toggle('btn-outline-dark', !isA);
        b.innerHTML = isA
          ? '<i class="fas fa-check me-1"></i> TERPILIH'
          : '<i class="fas fa-hand-pointer me-1"></i> PILIH';
      }
    });
  });
});
</script>
</body>
</html>
