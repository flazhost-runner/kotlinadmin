<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - User List</title>
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
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">User Management</h1>
      </div>
      <div class="tw-card p-0 overflow-hidden">
        <div class="px-6 py-4 border-b flex items-center justify-between">
          <h2 class="text-lg font-semibold" style="color:var(--primary)">User List</h2>
          <div class="btn-group btn-sm">
            <a class="btn btn-success btn-sm" href="/admin/v1/access/user/create">
              <i class="fas fa-fw fa-plus"></i> Add Data
            </a>
            <button class="btn btn-danger btn-sm" form="selection"
                    formaction="/admin/v1/access/user/delete_selected?_csrf=${_csrf}"
                    data-confirm="Delete selected users?">
              <i class="fas fa-fw fa-times"></i> Delete Selected
            </button>
          </div>
        </div>
        <div class="p-4" style="overflow-x:auto">
          <table class="table table-bordered table-hover align-middle">
            <thead>
              <tr>
                <th></th>
                <th>
                  <form id="searchform" method="GET" action="/admin/v1/access/user">
                    <select name="q_page_size" class="form-control" style="min-width:70px" onchange="this.form.submit()">
                      <option value="10" <#if (filter.q_page_size!"10") == "10">selected</#if>>10</option>
                      <option value="20" <#if (filter.q_page_size!"") == "20">selected</#if>>20</option>
                      <option value="50" <#if (filter.q_page_size!"") == "50">selected</#if>>50</option>
                      <option value="100" <#if (filter.q_page_size!"") == "100">selected</#if>>100</option>
                    </select>
                  </form>
                </th>
                <th><input type="text" form="searchform" name="q_code" value="${(filter.q_code)!""}" placeholder="Code" class="form-control" style="min-width:80px"></th>
                <th><input type="text" form="searchform" name="q_name" value="${(filter.q_name)!""}" placeholder="Name" class="form-control" style="min-width:100px"></th>
                <th><input type="text" form="searchform" name="q_phone" value="${(filter.q_phone)!""}" placeholder="Phone" class="form-control" style="min-width:90px"></th>
                <th><input type="text" form="searchform" name="q_email" value="${(filter.q_email)!""}" placeholder="Email" class="form-control" style="min-width:120px"></th>
                <th>
                  <select form="searchform" name="q_status" class="form-control" style="min-width:90px">
                    <option value="">All Status</option>
                    <option value="Active" <#if (filter.q_status!"") == "Active">selected</#if>>Active</option>
                    <option value="Inactive" <#if (filter.q_status!"") == "Inactive">selected</#if>>Inactive</option>
                  </select>
                </th>
                <th><input type="text" form="searchform" name="q_role" value="${(filter.q_role)!""}" placeholder="Role" class="form-control" style="min-width:80px"></th>
                <th></th>
                <th>
                  <div class="btn-group">
                    <button form="searchform" class="btn btn-sm btn-success"><i class="fas fa-search"></i> Search</button>
                    <a href="/admin/v1/access/user" class="btn btn-sm btn-danger"><i class="fas fa-times"></i> Reset</a>
                  </div>
                </th>
              </tr>
              <tr>
                <th><input type="checkbox" id="checkall"></th>
                <th>No</th>
                <th>Code</th>
                <th>Name</th>
                <th>Phone</th>
                <th>Email</th>
                <th>Status</th>
                <th>Roles</th>
                <th>Picture</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <form id="selection" method="POST">
                <input type="hidden" name="_csrf" value="${_csrf}">
              </form>
              <#if datas?? && datas?size gt 0>
                <#list datas as item>
                <tr>
                  <td><input type="checkbox" form="selection" name="selected[]" value="${item.id}"></td>
                  <td>${(paginate_data.page - 1) * paginate_data.pageSize + item?index + 1}</td>
                  <td><code class="text-xs">${item.code!""}</code></td>
                  <td class="font-medium">${item.name!""}</td>
                  <td>${item.phone!""}</td>
                  <td>${item.email!""}</td>
                  <td>
                    <#if (item.status!"") == "Active">
                      <i class="fas fa-check-circle text-green-500 text-xl"></i>
                    <#else>
                      <i class="fas fa-times-circle text-red-500 text-xl"></i>
                    </#if>
                  </td>
                  <td>
                    <#if item.roles?? && item.roles?size gt 0>
                      <#list item.roles as role>
                        <span class="badge text-bg-primary me-1">${role.name!""}</span>
                      </#list>
                    </#if>
                  </td>
                  <td>
                    <img src="${item.picture!""}" alt="picture" width="40" height="40"
                         class="rounded object-cover" style="max-width:100px;max-height:100px">
                  </td>
                  <td>
                    <div class="btn-group relative">
                      <button class="btn btn-sm btn-primary dropdown-toggle" data-toggle-dd>Action</button>
                      <div class="dropdown-menu dropdown-menu-end">
                        <a class="dropdown-item" href="/admin/v1/access/user/${item.id}/edit">
                          <i class="fas fa-edit"></i> Edit
                        </a>
                        <div class="dropdown-divider"></div>
                        <button class="dropdown-item text-danger" form="delete-${item.id}"
                                data-confirm="Confirm Delete">
                          <i class="fas fa-trash"></i> Delete
                        </button>
                      </div>
                    </div>
                    <form id="delete-${item.id}" method="POST"
                          action="/admin/v1/access/user/${item.id}/delete?_method=DELETE&_csrf=${_csrf}"></form>
                  </td>
                </tr>
                </#list>
              <#else>
                <tr><td colspan="10" class="text-center text-gray-400 py-8">No data found</td></tr>
              </#if>
            </tbody>
          </table>
          <#if paginate_data??>
          <nav class="mt-4">
            <ul class="pagination">
              <li class="page-item <#if !paginate_data.hasPrev>disabled</#if>">
                <a class="page-link" href="?q_page=${paginate_data.page - 1}<#if (filter.q_name)?? && filter.q_name != "">&q_name=${filter.q_name}</#if><#if (filter.q_page_size)??>&q_page_size=${filter.q_page_size}</#if>">Previous</a>
              </li>
              <#list 1..paginate_data.totalPages as p>
                <#if p == 1 || p == paginate_data.totalPages || (p >= paginate_data.page - 2 && p <= paginate_data.page + 2)>
                  <li class="page-item <#if p == paginate_data.page>active</#if>">
                    <a class="page-link" href="?q_page=${p}<#if (filter.q_page_size)??>&q_page_size=${filter.q_page_size}</#if>">${p}</a>
                  </li>
                <#elseif p == 2 || p == paginate_data.totalPages - 1>
                  <li class="page-item disabled"><a class="page-link" href="#">…</a></li>
                </#if>
              </#list>
              <li class="page-item <#if !paginate_data.hasNext>disabled</#if>">
                <a class="page-link" href="?q_page=${paginate_data.page + 1}<#if (filter.q_page_size)??>&q_page_size=${filter.q_page_size}</#if>">Next</a>
              </li>
            </ul>
          </nav>
          </#if>
        </div>
      </div>
    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
<script>$("#checkall").click(function(){ $('input:checkbox').not(this).prop('checked', this.checked); });</script>
</body>
</html>
