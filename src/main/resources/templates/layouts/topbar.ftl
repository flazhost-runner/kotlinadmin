<#ftl output_format="HTML">
<div class="flex items-center justify-between px-4 py-3">
  <div class="flex items-center gap-3">
    <button id="sidebar-toggle" class="md:hidden text-gray-500 hover:text-gray-700 p-1">
      <i class="fas fa-bars text-xl"></i>
    </button>
    <a href="/" class="text-gray-500 hover:text-gray-700 p-1" title="Go to Home">
      <i class="fas fa-home text-lg"></i>
    </a>
  </div>

  <div class="flex items-center gap-3">
    <div class="relative">
      <button class="flex items-center gap-2 text-gray-700 hover:text-gray-900 p-1" data-toggle-dd id="user-dd-btn">
        <img src="${(user.picture)!""}" alt="user avatar" width="32" height="32"
             class="rounded-full w-8 h-8 object-cover border-2 border-gray-200">
        <span class="hidden sm:block text-sm font-medium">Welcome, ${(user.name)!"User"}</span>
        <i class="fas fa-cog text-gray-400"></i>
      </button>
      <div class="dropdown-menu" id="user-dropdown" style="min-width:180px">
        <a class="dropdown-item" href="/admin/v1/profile">
          <i class="fas fa-user-circle"></i> Profile
        </a>
        <div class="dropdown-divider"></div>
        <form method="POST" action="/auth/logout?_csrf=${_csrf}" style="margin:0">
          <button type="submit" class="dropdown-item text-red-600">
            <i class="fas fa-sign-out-alt"></i> Logout
          </button>
        </form>
      </div>
    </div>
  </div>
</div>

<script>
document.getElementById('sidebar-toggle')?.addEventListener('click', function() {
  const sidebar = document.querySelector('aside');
  if (sidebar) sidebar.classList.toggle('hidden');
});
</script>
