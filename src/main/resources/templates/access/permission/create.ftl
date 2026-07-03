<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Permission</title>
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
      <div class="tw-card p-6">
        <h2 class="text-lg font-bold mb-4" style="color:var(--primary)">Permission Form</h2>
        <#if flash??><#include "/layouts/flash.ftl"></#if>
        <form method="POST" action="/admin/v1/access/permission/store?_csrf=${_csrf}">
          <div class="mb-3">
            <label for="name" class="form-label fw-semibold">Name</label>
            <input id="name" type="text" class="form-control <#if (errors.name)??>is-invalid</#if>" name="name" value="${(old.name)!""}">
            <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
          </div>
          <div class="mb-3">
            <label for="guard_name" class="form-label fw-semibold">Guard</label>
            <select name="guard_name" id="guard_name" class="form-control">
              <option value="web" <#if (old.guard_name!"web") == "web">selected</#if>>web</option>
              <option value="api" <#if (old.guard_name!"") == "api">selected</#if>>api</option>
            </select>
          </div>
          <div class="mb-3">
            <label for="method" class="form-label fw-semibold">Method</label>
            <select id="method" name="method" class="form-control <#if (errors.method)??>is-invalid</#if>">
              <option value="">-- Select Method --</option>
              <option value="GET" <#if (old.method!"") == "GET">selected</#if>>GET</option>
              <option value="POST" <#if (old.method!"") == "POST">selected</#if>>POST</option>
              <option value="PUT" <#if (old.method!"") == "PUT">selected</#if>>PUT</option>
              <option value="PATCH" <#if (old.method!"") == "PATCH">selected</#if>>PATCH</option>
              <option value="DELETE" <#if (old.method!"") == "DELETE">selected</#if>>DELETE</option>
            </select>
            <#if (errors.method)??><div class="invalid-feedback">${errors.method}</div></#if>
          </div>
          <div class="mb-3">
            <label for="desc" class="form-label fw-semibold">Description</label>
            <input id="desc" type="text" class="form-control <#if (errors.desc)??>is-invalid</#if>" name="desc" value="${(old.desc)!""}">
            <#if (errors.desc)??><div class="invalid-feedback">${errors.desc}</div></#if>
          </div>
          <div class="mb-4">
            <label for="status" class="form-label fw-semibold">Status</label>
            <select name="status" id="status" class="form-control" required>
              <option value="Active" <#if (old.status!"Active") == "Active">selected</#if>>Active</option>
              <option value="Inactive" <#if (old.status!"") == "Inactive">selected</#if>>Inactive</option>
            </select>
          </div>
          <div class="d-flex gap-2">
            <button type="submit" class="btn btn-primary-tw px-4 py-2"><i class="fas fa-save me-1"></i> Save</button>
            <a href="/admin/v1/access/permission" class="btn btn-danger px-4 py-2 text-white">Back</a>
          </div>
        </form>
      </div>
    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
</body>
</html>
