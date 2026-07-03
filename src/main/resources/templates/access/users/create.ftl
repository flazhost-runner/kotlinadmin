<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Create User</title>
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
      <div class="tw-card p-6 max-w-3xl mx-auto">
        <div class="flex items-center justify-between mb-6">
          <h2 class="text-lg font-semibold" style="color:var(--primary)">User Form</h2>
          <a href="/admin/v1/access/user" class="btn btn-light btn-sm">
            <i class="fas fa-arrow-left"></i> Back
          </a>
        </div>

        <form method="POST" action="/admin/v1/access/user/store?_csrf=${_csrf}" enctype="multipart/form-data">
          <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label class="form-label">[code] Code</label>
              <input type="text" name="code" value="${(old.code)!""}"
                     class="form-control <#if (errors.code)??>is-invalid</#if>"
                     placeholder="USR-001" required>
              <#if (errors.code)??><div class="invalid-feedback">${errors.code}</div></#if>
            </div>
            <div>
              <label class="form-label">[name] Name</label>
              <input type="text" name="name" value="${(old.name)!""}"
                     class="form-control <#if (errors.name)??>is-invalid</#if>"
                     placeholder="Full Name" required>
              <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
            </div>
            <div>
              <label class="form-label">[phone] Phone</label>
              <input type="text" name="phone" value="${(old.phone)!""}"
                     class="form-control <#if (errors.phone)??>is-invalid</#if>"
                     placeholder="+628xxxxxxxxx">
              <#if (errors.phone)??><div class="invalid-feedback">${errors.phone}</div></#if>
            </div>
            <div>
              <label class="form-label">[email] Email</label>
              <input type="email" name="email" value="${(old.email)!""}"
                     class="form-control <#if (errors.email)??>is-invalid</#if>"
                     placeholder="user@example.com" required>
              <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
            </div>
            <div>
              <label class="form-label">[password] Password</label>
              <input type="password" name="password"
                     class="form-control <#if (errors.password)??>is-invalid</#if>"
                     placeholder="Minimum 8 characters" required>
              <#if (errors.password)??><div class="invalid-feedback">${errors.password}</div></#if>
            </div>
            <div>
              <label class="form-label">[password_confirm] Confirm Password</label>
              <input type="password" name="password_confirmation"
                     class="form-control <#if (errors.password_confirmation)??>is-invalid</#if>"
                     placeholder="Repeat password" required>
              <#if (errors.password_confirmation)??><div class="invalid-feedback">${errors.password_confirmation}</div></#if>
            </div>
            <div>
              <label class="form-label">[status] Status</label>
              <select name="status" class="form-control <#if (errors.status)??>is-invalid</#if>">
                <option value="Active" <#if (old.status!"Active") == "Active">selected</#if>>Active</option>
                <option value="Inactive" <#if (old.status!"") == "Inactive">selected</#if>>Inactive</option>
              </select>
              <#if (errors.status)??><div class="invalid-feedback">${errors.status}</div></#if>
            </div>
            <div>
              <label class="form-label">[timezone] Timezone</label>
              <select name="timezone" class="form-control select2">
                <#assign timezones = ["UTC","Asia/Jakarta","Asia/Makassar","Asia/Jayapura","Asia/Singapore","America/New_York","America/Los_Angeles","Europe/London","Europe/Paris"]>
                <#list timezones as tz>
                  <option value="${tz}" <#if (old.timezone!"UTC") == tz>selected</#if>>${tz}</option>
                </#list>
              </select>
            </div>
            <div class="md:col-span-2">
              <label class="form-label">[roles] Roles</label>
              <div class="d-flex flex-wrap gap-3 p-2 rounded border">
                <#if roles??>
                  <#list roles as role>
                    <label class="d-flex align-items-center gap-2">
                      <input type="checkbox" name="roles[]" value="${role.id}" class="w-4 h-4">
                      <span>${role.name}</span>
                    </label>
                  </#list>
                </#if>
              </div>
            </div>
            <div class="md:col-span-2">
              <label class="form-label fw-semibold d-block">Blocked</label>
              <div class="d-flex flex-wrap gap-3 p-2 rounded border">
                <label class="d-flex align-items-center gap-2">
                  <input id="blocked" type="checkbox" name="blocked" value="1" class="w-4 h-4">
                  <span>Block account</span>
                </label>
              </div>
            </div>
            <div class="md:col-span-2" id="div_blocked_reason">
              <label class="form-label fw-semibold">Blocked Reason</label>
              <input id="blocked_reason" type="text" name="blocked_reason" class="form-control" value="">
            </div>
            <div class="md:col-span-2">
              <label class="form-label">[picture] Picture</label>
              <img id="picture-preview" src="" alt="picture preview" width="80" height="80"
                   class="rounded mb-2 object-cover border border-gray-200" style="display:block">
              <input type="file" name="picture" accept="image/*" class="form-control"
                     onchange="previewImage(this, 'picture-preview')">
              <#if (errors.picture)??><div class="invalid-feedback">${errors.picture}</div></#if>
            </div>
          </div>
          <div class="mt-6 flex gap-2">
            <button type="submit" class="btn btn-primary">
              <i class="fas fa-save"></i> Save User
            </button>
            <a href="/admin/v1/access/user" class="btn btn-danger">Cancel</a>
          </div>
        </form>
      </div>
    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
</body>
</html>
