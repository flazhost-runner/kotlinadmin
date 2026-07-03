<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Create Role</title>
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
        <h1 class="text-2xl font-bold text-gray-800">Role Management</h1>
      </div>
      <div class="tw-card p-6">
        <h2 class="text-lg font-bold mb-4" style="color:var(--primary)">Role Form</h2>
        <form method="POST" action="/admin/v1/access/role/store?_csrf=${_csrf}">
          <div class="mb-3">
            <label for="name" class="form-label fw-semibold">Name</label>
            <input id="name" type="text" name="name" value="${(old.name)!""}"
                   class="form-control <#if (errors.name)??>is-invalid</#if>">
            <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
          </div>
          <div class="mb-3">
            <label for="desc" class="form-label fw-semibold">Description</label>
            <input id="desc" type="text" name="description" value="${(old.description)!""}"
                   class="form-control <#if (errors.description)??>is-invalid</#if>">
            <#if (errors.description)??><div class="invalid-feedback">${errors.description}</div></#if>
          </div>
          <div class="mb-4">
            <label for="status" class="form-label fw-semibold">Status</label>
            <select id="status" name="status" class="form-control <#if (errors.status)??>is-invalid</#if>" required>
              <option value="Active" <#if (old.status!"Active") == "Active">selected</#if>>Active</option>
              <option value="Inactive" <#if (old.status!"") == "Inactive">selected</#if>>Inactive</option>
            </select>
            <#if (errors.status)??><div class="invalid-feedback">${errors.status}</div></#if>
          </div>
          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary-tw px-4 py-2"><i class="fas fa-save me-1"></i> Save</button>
            <a href="/admin/v1/access/role" class="btn btn-danger px-4 py-2 text-white">Back</a>
          </div>
        </form>
      </div>
    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
</body>
</html>
