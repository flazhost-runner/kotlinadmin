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

        <!-- Frontend Template (katalog opentailwind — paritas NodeAdmin/GoAdmin) -->
        <input type="hidden" id="fe_template_input" name="fe_template" value="${fe_active!setting.fe_template!""}">
        <div class="tw-card p-6 mb-6">
          <div class="flex items-center gap-2 mb-1">
            <i class="fas fa-window-maximize" style="color:var(--primary)"></i>
            <h2 class="text-lg font-semibold" style="color:var(--primary)">Frontend Template</h2>
          </div>
          <p class="text-sm text-gray-500 mb-3">
            Pick a public landing design from
            <a href="https://github.com/lindoai/opentailwind" target="_blank" class="underline">opentailwind</a>
            (${fe_total!0} templates). Click <b>Preview</b> for full view. The selected template is
            downloaded &amp; cached on <b>Save</b>. See it on the
            <a href="/" target="_blank" class="underline" style="color:var(--primary)">landing page ↗</a>.
          </p>
          <form id="fe_search" method="GET" action="/admin/v1/setting"></form>
          <div class="d-flex flex-wrap gap-2 mb-3">
            <input form="fe_search" type="text" name="fe_search" value="${fe_search!""}" placeholder="Search templates…" class="form-control" style="max-width:220px">
            <select form="fe_search" name="fe_category" class="form-control" style="max-width:200px">
              <option value="">All categories</option>
              <#list (fe_categories![]) as c>
              <option value="${c}" <#if (fe_category!"") == c>selected</#if>>${c}</option>
              </#list>
            </select>
            <button form="fe_search" type="submit" class="btn btn-success btn-sm"><i class="fas fa-search me-1"></i> Search</button>
            <a href="/admin/v1/setting" class="btn btn-danger btn-sm"><i class="fas fa-times me-1"></i> Reset</a>
          </div>

          <#if !(fe_catalog?has_content)>
          <div class="text-center text-gray-400 py-10"><i class="fas fa-search fa-2x mb-2"></i><p>No template matches your search.</p></div>
          </#if>

          <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
            <#list (fe_catalog![]) as t>
            <#assign feActiveCard = (fe_active!"") == t.slug>
            <div class="fe-card block" data-slug="${t.slug}">
              <div class="fe-swatch rounded-xl overflow-hidden border-2 transition <#if feActiveCard>border-gray-900<#else>border-gray-300</#if>" style="box-shadow:0 2px 8px rgba(0,0,0,.12)">
                <div class="fe-thumb fe-preview-trigger relative bg-gray-100 cursor-pointer group" data-slug="${t.slug}" data-name="${t.name}"
                     style="height:140px;overflow:hidden;border-bottom:1px solid #d1d5db;border-top-left-radius:.7rem;border-top-right-radius:.7rem;transform:translateZ(0)"
                     data-preview-url="/admin/v1/setting/fe-preview/${t.slug}">
                  <div class="fe-thumb-placeholder absolute inset-0 flex items-center justify-center text-gray-300"><i class="fas fa-spinner fa-spin"></i></div>
                  <div class="absolute inset-0 flex items-center justify-center opacity-0 group-hover:opacity-100 transition" style="background:rgba(0,0,0,.45);pointer-events:none">
                    <span class="text-white text-sm font-semibold"><i class="fas fa-eye me-1"></i> Preview</span>
                  </div>
                </div>
                <div class="bg-white py-2 px-3">
                  <div class="d-flex align-items-center justify-content-between">
                    <span class="text-sm fw-semibold text-gray-800 text-truncate" title="${t.name}">${t.name}</span>
                    <i class="fas fa-check-circle fe-check<#if !feActiveCard> hidden</#if>" style="color:var(--primary)"></i>
                  </div>
                  <span class="text-xs text-gray-400">${t.category}</span>
                  <button type="button" class="fe-select btn btn-sm w-100 mt-2 fw-bold <#if feActiveCard>btn-primary-tw<#else>btn-outline-dark</#if>" style="font-size:11px">
                    <#if feActiveCard>
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

          <#if (fe_last_page!1) gt 1>
          <#assign fePg = fe_page!1, feLast = fe_last_page!1>
          <#assign feWinStart = [1, fePg - 2]?max, feWinEnd = [feLast, fePg + 2]?min>
          <div class="d-flex justify-content-center mt-5">
            <nav><ul class="pagination">
              <#if (fePg > 1)><li class="page-item"><a class="page-link" href="?fe_page=${fePg - 1}&fe_search=${fe_search!""}&fe_category=${fe_category!""}">Previous</a></li></#if>
              <#if (feWinStart > 1)><li class="page-item"><a class="page-link" href="?fe_page=1&fe_search=${fe_search!""}&fe_category=${fe_category!""}">1</a></li><li class="page-item disabled"><span class="page-link">…</span></li></#if>
              <#list feWinStart..feWinEnd as pg>
              <li class="page-item <#if pg == fePg>active</#if>"><a class="page-link" href="?fe_page=${pg}&fe_search=${fe_search!""}&fe_category=${fe_category!""}">${pg}</a></li>
              </#list>
              <#if (feWinEnd < feLast)><li class="page-item disabled"><span class="page-link">…</span></li><li class="page-item"><a class="page-link" href="?fe_page=${feLast}&fe_search=${fe_search!""}&fe_category=${fe_category!""}">${feLast}</a></li></#if>
              <#if (fePg < feLast)><li class="page-item"><a class="page-link" href="?fe_page=${fePg + 1}&fe_search=${fe_search!""}&fe_category=${fe_category!""}">Next</a></li></#if>
            </ul></nav>
          </div>
          </#if>
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

// ── FE template switcher: lazy thumbnail + preview modal + pilihan persist ──
// (replika NodeAdmin setting switcher, via port RustAdmin/GoAdmin)
(function () {
  var LS_PREFIX = 'fe_tpl_html:', LS_SEL = 'fe_tpl_selected';
  var feInput = document.getElementById('fe_template_input');
  var savedSel = localStorage.getItem(LS_SEL);
  if (savedSel && feInput) feInput.value = savedSel;

  function forceLight(html) {
    var inject = '<meta name="color-scheme" content="light">' +
      '<style>:root{color-scheme:light !important}</style>';
    if (/<head[^>]*>/i.test(html)) { return html.replace(/<head[^>]*>/i, function (m) { return m + inject; }); }
    return inject + html;
  }
  function getHtml(slug, url) {
    var cached = null; try { cached = localStorage.getItem(LS_PREFIX + slug); } catch (e) {}
    if (cached) return Promise.resolve(cached);
    return fetch(url, { credentials: 'same-origin' })
      .then(function (r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.text(); })
      .then(function (html) { try { localStorage.setItem(LS_PREFIX + slug, html); } catch (e) {} return html; });
  }
  function renderThumb(box) {
    var slug = box.getAttribute('data-slug'), url = box.getAttribute('data-preview-url');
    getHtml(slug, url).then(function (html) {
      var ph = box.querySelector('.fe-thumb-placeholder'); if (ph) ph.remove();
      var ifr = document.createElement('iframe');
      ifr.setAttribute('scrolling', 'no'); ifr.setAttribute('loading', 'lazy');
      var DESIGN_W = 1280, scale = (box.clientWidth || 280) / DESIGN_W;
      ifr.style.cssText = 'width:' + DESIGN_W + 'px;height:' + Math.ceil(140 / scale) + 'px;border:0;transform:scale(' + scale + ');transform-origin:top left;pointer-events:none';
      ifr.srcdoc = forceLight(html); box.appendChild(ifr);
    }).catch(function () { var ph = box.querySelector('.fe-thumb-placeholder'); if (ph) ph.innerHTML = '<i class="fas fa-image fa-2x"></i>'; });
  }
  var thumbs = document.querySelectorAll('.fe-thumb');
  if ('IntersectionObserver' in window) {
    var io = new IntersectionObserver(function (entries) { entries.forEach(function (en) { if (en.isIntersecting) { renderThumb(en.target); io.unobserve(en.target); } }); }, { rootMargin: '200px' });
    thumbs.forEach(function (t) { io.observe(t); });
  } else { thumbs.forEach(renderThumb); }

  function selectSlug(slug) {
    if (feInput) feInput.value = slug;
    try { localStorage.setItem(LS_SEL, slug); } catch (e) {}
    document.querySelectorAll('.fe-card').forEach(function (card) {
      var isA = card.getAttribute('data-slug') === slug;
      var sw = card.querySelector('.fe-swatch'), ch = card.querySelector('.fe-check'), b = card.querySelector('.fe-select');
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
  }
  document.querySelectorAll('.fe-select').forEach(function (b) { b.addEventListener('click', function () { selectSlug(this.closest('.fe-card').getAttribute('data-slug')); }); });
  if (feInput && feInput.value) selectSlug(feInput.value);

  var modal = document.getElementById('fe-preview-modal'), frame = document.getElementById('fe-preview-frame'), title = document.getElementById('fe-preview-title');
  function openModal(slug, name, url) {
    title.textContent = name; frame.srcdoc = '<div style="font-family:sans-serif;padding:40px">Loading…</div>';
    modal.classList.remove('hidden'); modal.classList.add('flex');
    getHtml(slug, url).then(function (html) { frame.srcdoc = forceLight(html); }).catch(function () { frame.srcdoc = '<p style="padding:40px;font-family:sans-serif">Failed to load preview.</p>'; });
  }
  function closeModal() { modal.classList.add('hidden'); modal.classList.remove('flex'); frame.srcdoc = ''; }
  document.querySelectorAll('.fe-preview-trigger').forEach(function (b) { b.addEventListener('click', function () { openModal(this.getAttribute('data-slug'), this.getAttribute('data-name'), this.getAttribute('data-preview-url')); }); });
  var closeBtn = document.getElementById('fe-preview-close');
  if (closeBtn) closeBtn.addEventListener('click', closeModal);
  if (modal) modal.addEventListener('click', function (e) { if (e.target === modal) closeModal(); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') closeModal(); });
})();
</script>

<#-- Modal preview FE template (full view) -->
<div id="fe-preview-modal" class="hidden fixed inset-0 z-50 items-center justify-center" style="background:rgba(0,0,0,.6)">
  <div class="bg-white rounded-xl overflow-hidden shadow-2xl" style="width:92vw;height:90vh;display:flex;flex-direction:column">
    <div class="flex items-center justify-between px-4 py-3 border-b">
      <h3 id="fe-preview-title" class="font-bold text-gray-800">Preview</h3>
      <button id="fe-preview-close" type="button" class="btn btn-sm btn-danger"><i class="fas fa-times"></i> Close</button>
    </div>
    <iframe id="fe-preview-frame" class="flex-1 w-full" style="border:0"></iframe>
  </div>
</div>
</body>
</html>
