<#ftl output_format="HTML">

<#-- Modal overlay for confirmDialog() and openModal() -->
<div id="modal-overlay" class="modal-overlay" style="display:none;">
  <div id="modal-box" class="modal-box">
    <div class="modal-header">
      <span id="modal-title" class="modal-title">Confirm</span>
      <button class="modal-close" onclick="closeModal()">&times;</button>
    </div>
    <div id="modal-body" class="modal-body"></div>
    <div id="modal-footer" class="modal-footer" style="display:none;">
      <button id="modal-cancel" class="btn btn-light btn-sm" onclick="closeModal()">Cancel</button>
      <button id="modal-confirm" class="btn btn-danger btn-sm">Confirm</button>
    </div>
  </div>
</div>

<#-- Toast container -->
<div id="toast-container" class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 max-w-sm w-full pointer-events-none"></div>

<#-- Flash toast on page load -->
<#if flash??>
<script>
document.addEventListener('DOMContentLoaded', function() {
  window.Toast(${flash.message?js_string?json_string}, '${flash.key}');
});
</script>
</#if>

<script>
// window.Toast(message, type) — auto-dismiss 3500ms
window.Toast = function(message, type) {
  type = type || 'info';
  var container = document.getElementById('toast-container');
  var t = document.createElement('div');
  t.className = 'toast ' + type + ' show pointer-events-auto';
  t.innerHTML = '<span class="flex-1">' + message + '</span>' +
    '<button onclick="this.parentNode.remove()" class="ml-2 opacity-70 hover:opacity-100">&times;</button>';
  container.appendChild(t);
  setTimeout(function() {
    t.style.opacity = '0';
    t.style.transition = 'opacity 0.5s';
    setTimeout(function() { t.remove(); }, 500);
  }, 3500);
};

// confirmDialog(msg) — returns Promise<bool>, uses themed modal
window.confirmDialog = function(msg) {
  return new Promise(function(resolve) {
    var overlay = document.getElementById('modal-overlay');
    var body = document.getElementById('modal-body');
    var footer = document.getElementById('modal-footer');
    var confirmBtn = document.getElementById('modal-confirm');
    var title = document.getElementById('modal-title');
    title.textContent = 'Confirm';
    body.textContent = msg || 'Are you sure?';
    footer.style.display = '';
    openModal();
    confirmBtn.onclick = function() { closeModal(); resolve(true); };
    document.getElementById('modal-cancel').onclick = function() { closeModal(); resolve(false); };
  });
};

function openModal() {
  var overlay = document.getElementById('modal-overlay');
  overlay.style.display = 'flex';
  document.addEventListener('keydown', _modalEsc);
}

function closeModal() {
  var overlay = document.getElementById('modal-overlay');
  overlay.style.display = 'none';
  document.removeEventListener('keydown', _modalEsc);
}

function _modalEsc(e) { if (e.key === 'Escape') closeModal(); }

document.getElementById('modal-overlay').addEventListener('click', function(e) {
  if (e.target === this) closeModal();
});

// Replace data-confirm with themed confirmDialog
document.addEventListener('click', function(e) {
  var el = e.target.closest('[data-confirm]');
  if (!el) return;
  e.preventDefault();
  e.stopPropagation();
  var msg = el.getAttribute('data-confirm') || 'Are you sure?';
  window.confirmDialog(msg).then(function(ok) {
    if (!ok) return;
    if (el.tagName === 'A') { window.location.href = el.href; }
    else if (el.type === 'submit') { el.form && el.form.submit(); }
    else if (el.getAttribute('data-action')) {
      var f = document.getElementById(el.getAttribute('data-action'));
      if (f) f.submit();
    }
  });
});
</script>

<#-- Global image fallback -->
<script>
(function() {
  function handleImgError(img) {
    var isAvatar = img.classList.contains('rounded-full') ||
                   (img.alt && (img.alt.toLowerCase().includes('user') || img.alt.toLowerCase().includes('picture')));
    var icon = isAvatar ? 'fa-user' : 'fa-image';
    var w = img.width || img.offsetWidth || 48;
    var h = img.height || img.offsetHeight || 48;
    var span = document.createElement('span');
    span.className = 'inline-flex items-center justify-center bg-gray-200 rounded';
    span.style.width = w + 'px';
    span.style.height = h + 'px';
    span.innerHTML = '<i class="fas ' + icon + ' text-gray-400" style="font-size:' + Math.floor(w*0.4) + 'px"></i>';
    img.parentNode.replaceChild(span, img);
  }
  document.addEventListener('error', function(e) {
    if (e.target && e.target.tagName === 'IMG') handleImgError(e.target);
  }, true);
  document.addEventListener('DOMContentLoaded', function() {
    document.querySelectorAll('img').forEach(function(img) {
      if (img.complete && img.naturalWidth === 0) handleImgError(img);
    });
  });
})();

function previewImage(input, previewId, maxWidth) {
  if (!input.files || !input.files[0]) return;
  var reader = new FileReader();
  reader.onload = function(e) {
    var el = document.getElementById(previewId);
    if (el) {
      el.src = e.target.result;
      if (maxWidth) el.style.maxWidth = maxWidth;
      el.className = 'rounded border p-1';
    }
  };
  reader.readAsDataURL(input.files[0]);
}
</script>

<#-- Dropdown toggle -->
<script>
document.addEventListener('click', function(e) {
  var toggleBtn = e.target.closest('[data-toggle-dd]');
  if (toggleBtn) {
    var menu = toggleBtn.nextElementSibling;
    if (menu && menu.classList.contains('dropdown-menu')) {
      var wasOpen = menu.classList.contains('show');
      document.querySelectorAll('.dropdown-menu.show').forEach(function(m) { m.classList.remove('show'); });
      if (!wasOpen) menu.classList.add('show');
      e.stopPropagation();
      return;
    }
  }
  document.querySelectorAll('.dropdown-menu.show').forEach(function(m) { m.classList.remove('show'); });
});
</script>

<#-- Select2 init -->
<script src="https://cdn.jsdelivr.net/npm/select2@4/dist/js/select2.min.js"></script>
<script>$(document).ready(function() { $('.select2').select2({ width: '100%' }); });</script>

<#-- Trumbowyg rich text -->
<script src="https://cdnjs.cloudflare.com/ajax/libs/Trumbowyg/2.21.0/trumbowyg.min.js" integrity="sha512-l6MMck8/SpFCgbJnIEfVsWQ8MaNK/n2ppTiELW3I2BFY5pAm/WjkNHSt+2OD7+CZtygs+jr+dAgzNdjNuCU7kw==" crossorigin="anonymous"></script>
<script src="/be/default/vendor/trumbowyg/filemanager.js"></script>
<script>
(function () {
    if (!(window.jQuery && jQuery.fn.trumbowyg)) return;
    $('.trumbowyg').trumbowyg();
    $('.trumbowyg-editor').trumbowyg({
        btns: [
            ['viewHTML'],
            ['formatting'],
            ['strong', 'em', 'del'],
            ['superscript', 'subscript'],
            ['link'],
            ['filemanager'],
            ['justifyLeft', 'justifyCenter', 'justifyRight', 'justifyFull'],
            ['unorderedList', 'orderedList'],
            ['horizontalRule'],
            ['removeformat'],
            ['fullscreen']
        ],
        semantic: { div: 'div' },
        removeformatPasted: true,
        autogrow: true,
        plugins: { filemanager: true }
    });
    $('form').on('submit', function () {
        $(this).find('.trumbowyg, .trumbowyg-editor').each(function () {
            if ($(this).data('trumbowyg')) $(this).val($(this).trumbowyg('html'));
        });
    });
})();
</script>

<#-- Chart.js -->
<script src="https://cdn.jsdelivr.net/npm/chart.js@4/dist/chart.umd.min.js"></script>
