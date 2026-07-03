<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Role Permissions</title>
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
        <h1 class="text-2xl font-bold text-gray-800">Permission Management</h1>
      </div>
      <div class="tw-card p-0 overflow-hidden">
        <div class="px-6 py-4 border-b flex items-center justify-between">
          <h2 class="text-lg font-bold" style="color:var(--primary)">Permission List</h2>
          <div class="btn-group btn-sm">
            <button class="btn btn-info btn-sm" form="selection"
                    formaction="/admin/v1/access/role/${role.id}/permission/assign_selected?_csrf=${_csrf}"
                    data-confirm="Confirm Assign">
              <i class="fas fa-fw fa-check"></i> Assign Selected
            </button>
            <button class="btn btn-danger btn-sm" form="selection"
                    formaction="/admin/v1/access/role/${role.id}/permission/unassign_selected?_csrf=${_csrf}"
                    data-confirm="Confirm Unassign">
              <i class="fas fa-fw fa-times"></i> Unassign Selected
            </button>
          </div>
        </div>

        <div class="p-4" style="overflow-x:auto">
          <table class="table table-bordered table-hover align-middle">
            <thead>
              <form id="searchform" method="GET" action="/admin/v1/access/role/${role.id}/permission">
                <tr>
                  <th width="2%"></th>
                  <th width="7%">
                    <select name="q_page_size" class="form-control" onchange="this.form.submit()">
                      <option value="10" <#if (filter.q_page_size!"10") == "10">selected</#if>>10</option>
                      <option value="20" <#if (filter.q_page_size!"") == "20">selected</#if>>20</option>
                      <option value="50" <#if (filter.q_page_size!"") == "50">selected</#if>>50</option>
                      <option value="100" <#if (filter.q_page_size!"") == "100">selected</#if>>100</option>
                    </select>
                  </th>
                  <th width="20%"><input type="text" id="q_name" name="q_name" value="${(filter.q_name)!""}" placeholder="Name" class="form-control"></th>
                  <th width="10%">
                    <select name="q_status" class="form-control">
                      <option disabled <#if !(filter.q_status)?? || (filter.q_status!"") == "">selected</#if>>Select</option>
                      <option value="Active" <#if (filter.q_status!"") == "Active">selected</#if>>Active</option>
                      <option value="Inactive" <#if (filter.q_status!"") == "Inactive">selected</#if>>Inactive</option>
                    </select>
                  </th>
                  <th width="15%"><input type="text" id="q_desc" name="q_desc" value="${(filter.q_desc)!""}" placeholder="Description" class="form-control"></th>
                  <th width="5%" class="text-center align-middle">
                    <div class="btn-group">
                      <button form="searchform" type="submit" class="btn btn-sm btn-success"><i class="fas fa-fw fa-search"></i></button>
                      <a href="/admin/v1/access/role/${role.id}/permission" class="btn btn-sm btn-danger"><i class="fas fa-fw fa-times"></i></a>
                    </div>
                  </th>
                </tr>
                <tr>
                  <th width="5%"><input type="checkbox" id="checkall"></th>
                  <th width="5%">No</th>
                  <th width="20%">Name</th>
                  <th width="15%">Status</th>
                  <th width="10%">Description</th>
                  <th width="5%">Action</th>
                </tr>
              </form>
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
                  <td>${item.name!""}</td>
                  <td class="text-left">
                    <#if item.assigned!false>
                      <i class="fas fa-check-circle text-green-500 text-xl" title="Assigned"></i>
                    <#else>
                      <i class="fas fa-times-circle text-gray-300 text-xl" title="Not assigned"></i>
                    </#if>
                  </td>
                  <td>${item.description!""}</td>
                  <td class="text-center">
                    <div class="btn-group">
                      <button class="btn btn-sm btn-primary dropdown-toggle" data-toggle-dd aria-expanded="false">Action</button>
                      <div class="dropdown-menu dropdown-menu-end">
                        <a class="dropdown-item" href="/admin/v1/access/role/${role.id}/permission/${item.id}/assign">
                          <i class="fas fa-check fa-fw"></i> Assign
                        </a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item danger" href="/admin/v1/access/role/${role.id}/permission/${item.id}/unassign">
                          <i class="fas fa-times fa-fw"></i> Unassign
                        </a>
                      </div>
                    </div>
                  </td>
                </tr>
                </#list>
              <#else>
                <tr><td colspan="6" class="text-center text-gray-400 py-4">No permissions found.</td></tr>
              </#if>
            </tbody>
          </table>
          <#if paginate_data??>
          <div class="d-flex justify-content-end mt-4">
            <nav>
              <ul class="pagination">
                <#if paginate_data.hasPrev>
                <li class="page-item"><a class="page-link" href="?q_page=${paginate_data.page - 1}&q_page_size=${(filter.q_page_size)!"10"}">Previous</a></li>
                </#if>
                <#list 1..paginate_data.totalPages as p>
                  <li class="page-item <#if p == paginate_data.page>active</#if>">
                    <a class="page-link" href="?q_page=${p}&q_page_size=${(filter.q_page_size)!"10"}">${p}</a>
                  </li>
                </#list>
                <#if paginate_data.hasNext>
                <li class="page-item"><a class="page-link" href="?q_page=${paginate_data.page + 1}&q_page_size=${(filter.q_page_size)!"10"}">Next</a></li>
                </#if>
              </ul>
            </nav>
          </div>
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
