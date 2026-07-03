<#ftl output_format="HTML">
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<meta name="csrf-token" content="${_csrf}">
<title>${setting.name!"KotlinAdmin"} - ${pageTitle!"Admin"}</title>
<link rel="stylesheet" href="/be/default/vendor/fontawesome-free/css/all.min.css">
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">
<link href="https://cdn.jsdelivr.net/npm/select2@4/dist/css/select2.min.css" rel="stylesheet">
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/Trumbowyg/2.21.0/ui/trumbowyg.min.css" integrity="sha512-XjpikIIW1P7jUS8ZWIznGs9KHujZQxhbnEsqMVQ5GBTTRmmJe32+ULipOxFePB8F8j9ahKmCjyJJ22VNEX60yg==" crossorigin="anonymous">
<script src="https://cdn.tailwindcss.com"></script>
<script>
tailwind.config = {
  theme: {
    extend: {
      colors: {
        primary: 'var(--primary)',
        secondary: 'var(--secondary)',
        'theme-light': 'var(--theme-light)',
        'theme-dark': 'var(--theme-dark)'
      }
    }
  }
}
</script>
<style type="text/tailwindcss">
  :root {
    --primary: ${theme.primary!"#3B82F6"};
    --secondary: ${theme.secondary!"#60A5FA"};
    --theme-light: ${theme.light!"#DBEAFE"};
    --theme-dark: ${theme.dark!"#1E40AF"};
  }
  body { @apply bg-gray-100 font-sans; }
  .btn { @apply inline-flex items-center justify-center px-3 py-1.5 rounded text-sm font-medium cursor-pointer border-0 transition-colors gap-1; }
  .btn-primary { background-color: var(--primary); @apply text-white hover:opacity-90; }
  .btn-secondary { background-color: var(--secondary); @apply text-white hover:opacity-90; }
  .btn-success { @apply bg-green-600 text-white hover:bg-green-700; }
  .btn-danger { @apply bg-red-600 text-white hover:bg-red-700; }
  .btn-warning { @apply bg-yellow-500 text-white hover:bg-yellow-600; }
  .btn-info { @apply bg-sky-500 text-white hover:bg-sky-600; }
  .btn-light { @apply bg-gray-100 text-gray-700 hover:bg-gray-200 border border-gray-300; }
  .btn-sm { @apply px-2 py-1 text-xs; }
  .btn-lg { @apply px-4 py-2.5 text-base; }
  .btn-group { @apply inline-flex; }
  .btn-group > .btn:not(:last-child) { @apply rounded-r-none; }
  .btn-group > .btn:not(:first-child) { @apply rounded-l-none border-l border-white/20; }
  .form-control { @apply w-full border border-gray-300 rounded px-3 py-1.5 text-sm focus:outline-none focus:border-[color:var(--primary)] focus:ring-1 focus:ring-[color:var(--primary)] bg-white; }
  .form-control:disabled { @apply bg-gray-100 cursor-not-allowed; }
  .form-label { @apply block text-sm font-medium text-gray-700 mb-1; }
  .form-check-input { @apply w-4 h-4 accent-[var(--primary)]; }
  .is-invalid { @apply border-red-500 focus:ring-red-300; }
  .invalid-feedback { @apply text-red-500 text-xs mt-1 block; }
  .table { @apply w-full text-sm border-collapse; }
  .table th { @apply px-3 py-2 text-left font-medium text-gray-600 bg-gray-50; }
  .table td { @apply px-3 py-2 text-gray-700; }
  .table-bordered { @apply border border-gray-200; }
  .table-bordered th, .table-bordered td { @apply border border-gray-200; }
  .table-hover tbody tr:hover { @apply bg-gray-50; }
  .align-middle td, .align-middle th { @apply align-middle; }
  .tw-card { @apply bg-white rounded-lg shadow-sm border border-gray-100; }
  .badge { @apply inline-flex items-center px-2 py-0.5 rounded text-xs font-medium; }
  .text-bg-primary { background-color: var(--primary); @apply text-white; }
  .text-bg-secondary { background-color: var(--secondary); @apply text-white; }
  .text-bg-success { @apply bg-green-100 text-green-800; }
  .text-bg-danger { @apply bg-red-100 text-red-800; }
  .text-bg-warning { @apply bg-yellow-100 text-yellow-800; }
  .text-bg-info { @apply bg-sky-100 text-sky-800; }
  .text-bg-light { @apply bg-gray-100 text-gray-700; }
  .alert { @apply p-4 rounded-lg mb-4 flex items-start gap-2; }
  .alert-success { @apply bg-green-50 text-green-800 border border-green-200; }
  .alert-danger { @apply bg-red-50 text-red-800 border border-red-200; }
  .alert-warning { @apply bg-yellow-50 text-yellow-800 border border-yellow-200; }
  .alert-info { @apply bg-sky-50 text-sky-800 border border-sky-200; }
  .alert-primary { background-color: var(--theme-light); color: var(--theme-dark); @apply border; border-color: var(--primary); }
  .pagination { @apply flex list-none gap-1 flex-wrap p-0 m-0; }
  .page-item {}
  .page-link { @apply flex items-center justify-center px-3 py-1.5 text-sm rounded border border-gray-300 hover:bg-gray-50 cursor-pointer text-gray-600 no-underline; }
  .page-item.active .page-link { background-color: var(--primary); @apply text-white border-[color:var(--primary)]; }
  .page-item.disabled .page-link { @apply opacity-40 cursor-not-allowed pointer-events-none; }
  .dropdown-menu { @apply hidden absolute right-0 mt-1 min-w-48 bg-white rounded-lg shadow-lg border border-gray-200 z-50 py-1; }
  .dropdown-menu.show { @apply block; }
  .dropdown-item { @apply flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-100 cursor-pointer w-full text-left no-underline; }
  .dropdown-item.text-danger, .dropdown-item.danger { @apply text-red-600 hover:bg-red-50; }
  .dropdown-item.danger:hover { background-color: rgb(254 242 242); color: rgb(220 38 38); }
  .tb-fm-img { max-width: 80px; max-height: 60px; object-fit: cover; }
  .tb-fm-selected { border: 2px solid var(--primary); }
  .modal-overlay { @apply fixed inset-0 z-[9998] flex items-center justify-center bg-black/50; }
  .modal-box { @apply bg-white rounded-lg shadow-xl max-w-md w-full mx-4 overflow-hidden; }
  .modal-header { @apply flex items-center justify-between px-6 py-4 border-b border-gray-200; }
  .modal-title { @apply font-semibold text-gray-800; }
  .modal-body { @apply px-6 py-4 text-gray-700; }
  .modal-footer { @apply flex justify-end gap-2 px-6 py-4 border-t border-gray-100; }
  .modal-close { @apply text-gray-400 hover:text-gray-600 text-xl leading-none bg-transparent border-0 cursor-pointer; }
  .toast { @apply flex items-center gap-2 px-4 py-3 rounded-lg shadow-lg border text-sm; }
  .toast.success { @apply bg-green-50 text-green-800 border-green-200; }
  .toast.error, .toast.danger { @apply bg-red-50 text-red-800 border-red-200; }
  .toast.info { @apply bg-sky-50 text-sky-800 border-sky-200; }
  .toast.warning { @apply bg-yellow-50 text-yellow-800 border-yellow-200; }
  .text-primary-tw { color: var(--primary); }
  .text-decoration-none { @apply no-underline; }
  .fw-semibold { @apply font-semibold; }
  .w-100 { @apply w-full; }
  .ps-3 { @apply pl-3; }
  .form-check { @apply flex items-center gap-2; }
  .form-check-label { @apply text-sm text-gray-600 cursor-pointer; }
  .small { @apply text-sm; }
  .sr-only { @apply sr-only; }
  .dropdown-divider { @apply border-t border-gray-200 my-1; }
  .sidebar-gradient { background: linear-gradient(180deg, var(--theme-dark) 0%, var(--primary) 100%); }
  .nav-link-tw { @apply flex items-center gap-3 px-4 py-2.5 text-white/80 hover:text-white hover:bg-white/10 rounded-lg mx-2 text-sm transition-all; }
  .nav-link-tw.active { @apply text-white bg-white/20; }
  .row { @apply flex flex-wrap -mx-2; }
  .col-md-6 { @apply w-full md:w-1/2 px-2; }
  .col-md-4 { @apply w-full md:w-1/3 px-2; }
  .col-md-3 { @apply w-full md:w-1/4 px-2; }
  .col-md-8 { @apply w-full md:w-2/3 px-2; }
  .col-12 { @apply w-full px-2; }
  .d-flex { @apply flex; }
  .align-items-center { @apply items-center; }
  .justify-content-between { @apply justify-between; }
  .me-2 { @apply mr-2; }
  .ms-2 { @apply ml-2; }
  .mb-3 { @apply mb-3; }
  .mb-4 { @apply mb-4; }
  .mt-4 { @apply mt-4; }
  .g-3 { @apply gap-3; }
  select.form-control { @apply appearance-none; }
  .btn-primary-tw { background-color: var(--primary); @apply text-white px-6 py-2 rounded font-medium hover:opacity-90 transition-opacity cursor-pointer border-0; }
</style>
<script src="https://code.jquery.com/jquery-3.7.1.min.js"></script>
