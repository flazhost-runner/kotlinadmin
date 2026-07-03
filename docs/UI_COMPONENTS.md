# UI Components — KotlinAdmin

Katalog komponen UI yang tersedia di admin panel. Semua komponen menggunakan **Tailwind CSS** (via CDN, dengan re-implementasi kelas gaya Bootstrap via `@apply`) dan **FontAwesome** untuk ikon.

Lihat halaman hidup: `/admin/v1/components`

---

## 1. Buttons

```html
<!-- Primary -->
<button class="btn btn-primary">Primary</button>
<button class="btn btn-primary btn-sm">Small Primary</button>

<!-- Variants -->
<button class="btn btn-success">Success</button>
<button class="btn btn-danger">Danger</button>
<button class="btn btn-warning">Warning</button>
<button class="btn btn-secondary">Secondary</button>

<!-- With icon -->
<button class="btn btn-primary">
  <i class="fas fa-plus me-1"></i> Add Data
</button>

<!-- Button Group -->
<div class="btn-group">
  <button class="btn btn-sm btn-success"><i class="fas fa-search"></i></button>
  <a class="btn btn-sm btn-danger" href="/reset"><i class="fas fa-times"></i></a>
</div>

<!-- Dropdown Button -->
<div class="btn-group">
  <button class="btn btn-sm btn-primary dropdown-toggle" data-toggle-dd>Action</button>
  <div class="dropdown-menu dropdown-menu-end">
    <a class="dropdown-item" href="/edit">Edit</a>
    <div class="dropdown-divider"></div>
    <button class="dropdown-item text-red-600" data-confirm="Sure?">Delete</button>
  </div>
</div>
```

CSS classes: `.btn .btn-sm .btn-primary .btn-success .btn-danger .btn-warning .btn-secondary .btn-group`

---

## 2. Badges & Status

```html
<!-- Badge (roles, method) -->
<span class="badge text-bg-primary">Administrator</span>
<span class="badge text-bg-success">GET</span>
<span class="badge text-bg-danger">DELETE</span>
<span class="badge text-bg-warning">PUT</span>
<span class="badge text-bg-secondary">POST</span>

<!-- Status Icons (gunakan ikon, bukan teks) -->
<!-- Active -->
<i class="fas fa-check-circle text-green-500 text-xl"></i>
<!-- Inactive -->
<i class="fas fa-times-circle text-red-500 text-xl"></i>
```

---

## 3. Alerts

```html
<div class="alert alert-success">
  <i class="fas fa-check-circle me-2"></i> Operation successful.
</div>
<div class="alert alert-danger">
  <i class="fas fa-exclamation-circle me-2"></i> An error occurred.
</div>
<div class="alert alert-warning">
  <i class="fas fa-exclamation-triangle me-2"></i> Warning message.
</div>
<div class="alert alert-info">
  <i class="fas fa-info-circle me-2"></i> Info message.
</div>
```

Flash toast (injected via `foot.ftl`):
```ftl
<#if flash??>
  <!-- Toast shown automatically -->
</#if>
```

---

## 4. Stat Cards

```html
<div class="tw-card p-6 flex items-center gap-4">
  <div class="rounded-full p-3" style="background:var(--theme-light)">
    <i class="fas fa-users fa-2x" style="color:var(--primary)"></i>
  </div>
  <div>
    <div class="text-3xl font-bold counter" data-target="42">0</div>
    <div class="text-gray-500 text-sm">Total Users</div>
  </div>
</div>
```

Animasi counter: `<span class="counter" data-target="100">0</span>` + JS di `foot.ftl`.

---

## 5. Data Table (Kanonik)

Semua tabel index WAJIB menggunakan struktur ini (sesuaikan kolom data saja):

```html
<div class="tw-card p-0 overflow-hidden">
  <div class="px-6 py-4 border-b flex items-center justify-between">
    <h2 style="color:var(--primary)">Resource List</h2>
    <div class="btn-group btn-sm">
      <a class="btn btn-success btn-sm" href="/create"><i class="fas fa-plus"></i> Add Data</a>
      <button class="btn btn-danger btn-sm" form="selection"
              formaction="/delete_selected?_csrf=${_csrf}"
              data-confirm="Delete selected?">
        <i class="fas fa-times"></i> Delete Selected
      </button>
    </div>
  </div>
  <div class="p-4" style="overflow-x:auto">
    <table class="table table-bordered table-hover align-middle">
      <thead>
        <!-- BARIS 1: FILTER -->
        <tr>
          <th></th>
          <th>
            <form id="searchform" method="GET">
            <select name="q_page_size" class="form-control form-control-sm" onchange="this.form.submit()">
              <#list [10,20,50,100] as s>
                <option value="${s}" <#if (filter["q_page_size"]!"")==s?string>selected</#if>>${s}</option>
              </#list>
            </select>
          </th>
          <!-- filter per kolom -->
          <th><input class="form-control form-control-sm" name="q_name" placeholder="Name"
                     value="${(filter["q_name"])!}"></th>
          <th>
            <div class="btn-group">
              <button class="btn btn-sm btn-success" type="submit" form="searchform">
                <i class="fas fa-search"></i>
              </button>
              <a class="btn btn-sm btn-danger" href="/reset"><i class="fas fa-times"></i></a>
            </div>
          </th>
        </tr>
        <!-- BARIS 2: HEADER -->
        <tr>
          <th><input type="checkbox" id="checkall"></th>
          <th>No</th>
          <th>Name</th>
          <th>Status</th>
          <th>Action</th>
        </tr>
      </thead>
      <tbody>
        <form id="selection" method="POST">
        <#list datas as item>
        <tr>
          <td><input type="checkbox" name="selected[]" value="${item.id.value}"></td>
          <td>${paginate_data.offset + item?index + 1}</td>
          <td>${item.name}</td>
          <td>
            <#if item.status == "Active">
              <i class="fas fa-check-circle text-green-500 text-xl"></i>
            <#else>
              <i class="fas fa-times-circle text-red-500 text-xl"></i>
            </#if>
          </td>
          <td>
            <div class="btn-group">
              <button class="btn btn-sm btn-primary dropdown-toggle" data-toggle-dd>Action</button>
              <div class="dropdown-menu dropdown-menu-end">
                <a class="dropdown-item" href="/${item.id.value}/edit">Edit</a>
                <div class="dropdown-divider"></div>
                <form method="POST" action="/${item.id.value}/delete?_method=DELETE&_csrf=${_csrf}">
                  <button class="dropdown-item text-red-600" data-confirm="Delete?">Delete</button>
                </form>
              </div>
            </div>
          </td>
        </tr>
        </#list>
        </form>
      </tbody>
    </table>
    <#include "../layouts/pagination.ftl">
  </div>
</div>

<script>
$("#checkall").click(function(){ $('input:checkbox').not(this).prop('checked', this.checked); });
</script>
```

---

## 6. Forms

```html
<!-- Text input -->
<div class="mb-3">
  <label class="form-label">Name <span class="text-red-500">*</span></label>
  <input type="text" name="name" class="form-control <#if errors.name??>is-invalid</#if>"
         value="${(old.name)!}">
  <#if errors.name??><div class="invalid-feedback">${errors.name}</div></#if>
</div>

<!-- Select -->
<div class="mb-3">
  <label class="form-label">Status</label>
  <select name="status" class="form-control">
    <option value="Active" <#if (old.status!'Active')=="Active">selected</#if>>Active</option>
    <option value="Inactive" <#if (old.status!"")==("Inactive")>selected</#if>>Inactive</option>
  </select>
</div>

<!-- File input (POLOS — native browser button, JANGAN style custom) -->
<div class="mb-3">
  <label class="form-label">Picture</label>
  <img src="${(data.picture)!}" id="preview-picture" style="max-width:80px;max-height:80px;display:block;margin-bottom:4px">
  <input type="file" name="picture" class="form-control" accept="image/*"
         onchange="previewImage(this,'preview-picture')">
</div>

<!-- Rich Text (Trumbowyg) -->
<div class="mb-3">
  <label class="form-label">Description</label>
  <textarea name="description" class="trumbowyg-editor form-control" rows="4">${(data.description)!}</textarea>
</div>
```

**Aturan input file:**
- Class `form-control` POLOS — JANGAN style `::file-selector-button`
- `<img>` preview SELALU dirender tanpa guard `<#if>` — fallback gambar global di `foot.ftl` menangani src kosong/rusak

---

## 7. Pagination

```ftl
<!-- layouts/pagination.ftl -->
<#if (paginate_data.totalPages > 1)>
<nav class="mt-4">
  <ul class="pagination">
    <li class="page-item <#if !paginate_data.hasPrev>disabled</#if>">
      <a class="page-link" href="?${currentQuery}&q_page=${paginate_data.page - 1}">Previous</a>
    </li>
    <!-- Windowed pagination: 1 ... cur-2..cur+2 ... last -->
    <#if (paginate_data.page > 3)>
      <li class="page-item"><a class="page-link" href="?${currentQuery}&q_page=1">1</a></li>
      <#if (paginate_data.page > 4)><li class="page-item disabled"><span class="page-link">…</span></li></#if>
    </#if>
    <#list [paginate_data.page-2, 1]?max..[paginate_data.page+2, paginate_data.totalPages]?min as p>
      <li class="page-item <#if p == paginate_data.page>active</#if>">
        <a class="page-link" href="?${currentQuery}&q_page=${p}">${p}</a>
      </li>
    </#list>
    <#if (paginate_data.page < paginate_data.totalPages - 2)>
      <#if (paginate_data.page < paginate_data.totalPages - 3)>
        <li class="page-item disabled"><span class="page-link">…</span></li>
      </#if>
      <li class="page-item">
        <a class="page-link" href="?${currentQuery}&q_page=${paginate_data.totalPages}">${paginate_data.totalPages}</a>
      </li>
    </#if>
    <li class="page-item <#if !paginate_data.hasNext>disabled</#if>">
      <a class="page-link" href="?${currentQuery}&q_page=${paginate_data.page + 1}">Next</a>
    </li>
  </ul>
</nav>
</#if>
```

---

## 8. Modal (JS vanilla)

```html
<!-- Trigger -->
<button class="btn btn-primary" data-modal-open="my-modal">Open Modal</button>

<!-- Modal -->
<div id="my-modal" class="modal-overlay hidden fixed inset-0 bg-black/50 z-50">
  <div class="modal-box tw-card max-w-lg mx-auto mt-24 p-6">
    <h3 class="text-lg font-bold mb-4">Modal Title</h3>
    <p>Modal content here.</p>
    <div class="flex justify-end gap-2 mt-4">
      <button class="btn btn-secondary" data-modal-close="my-modal">Cancel</button>
      <button class="btn btn-primary" id="modal-confirm">Confirm</button>
    </div>
  </div>
</div>
```

JS global di `foot.ftl`: `[data-modal-open]` → show, `[data-modal-close]` → hide, `[data-confirm]` → confirm dialog.

---

## 9. Toast

```html
<!-- Auto-shown via flash session -->
<!-- Di foot.ftl: -->
<div id="toast-container" class="fixed top-4 right-4 z-50 flex flex-col gap-2"></div>
```

```javascript
// Show toast programmatically
showToast('success', 'Operation completed!');
showToast('danger', 'An error occurred.');
showToast('warning', 'Please check your input.');
showToast('info', 'FYI: something happened.');
```

---

## 10. Charts (Chart.js, Themeable)

```html
<canvas id="lineChart" height="80"></canvas>

<script>
const theme = { primary: '${theme.primary}', secondary: '${theme.secondary}' };
new Chart(document.getElementById('lineChart'), {
  type: 'line',
  data: {
    labels: ['Jan','Feb','Mar','Apr','May','Jun'],
    datasets: [{
      label: 'Users',
      data: [12, 19, 3, 5, 2, 3],
      borderColor: theme.primary,
      backgroundColor: theme.primary + '20',
      tension: 0.4
    }]
  },
  options: { responsive: true, plugins: { legend: { position: 'top' } } }
});
</script>
```

Chart warna selalu mengacu `var(--primary)` / `var(--secondary)` — ikut tema aktif.

---

## 11. Image Preview (Input File)

```javascript
// Global helper di foot.ftl
function previewImage(input, imgId) {
    const file = input.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = e => document.getElementById(imgId).src = e.target.result;
        reader.readAsDataURL(file);
    }
}

// Fallback gambar gagal (global, app-wide)
document.addEventListener('error', function(e) {
    const img = e.target;
    if (img.tagName !== 'IMG') return;
    if (img.complete && img.naturalWidth === 0 || !img.src || img.src === window.location.href) {
        const isAvatar = /user|picture|avatar|rounded-full/i.test(img.className + img.alt);
        img.outerHTML = `<span class="img-placeholder flex items-center justify-center bg-gray-100 rounded"
            style="width:${img.width||80}px;height:${img.height||80}px">
            <i class="fas ${isAvatar ? 'fa-user' : 'fa-image'} text-gray-400 text-2xl"></i>
        </span>`;
    }
}, true);
```
