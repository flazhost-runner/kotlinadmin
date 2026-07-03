<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Profile</title>
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
      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

        <!-- Avatar Card -->
        <div class="tw-card p-6 text-center">
          <img id="avatar-preview" src="${(user.picture)!""}" alt="${(user.name)!""}"
               width="120" height="120"
               class="rounded-full mx-auto mb-4 object-cover border-4 border-white shadow-md" style="display:block">
          <h3 class="font-semibold text-lg">${(user.name)!""}</h3>
          <p class="text-gray-500 text-sm">${(user.email)!""}</p>
          <#if user.roles?? && user.roles?size gt 0>
            <div class="flex flex-wrap gap-1 justify-center mt-2">
              <#list user.roles as r>
                <span class="badge text-bg-info">${r.name}</span>
              </#list>
            </div>
          </#if>
          <form method="POST" action="/admin/v1/profile/picture?_csrf=${_csrf}" enctype="multipart/form-data" class="mt-4">
            <label class="btn btn-light btn-sm cursor-pointer">
              <i class="fas fa-camera"></i> Change Photo
              <input type="file" name="picture" accept="image/*" class="hidden"
                     onchange="previewImage(this, 'avatar-preview'); this.form.submit()">
            </label>
          </form>
        </div>

        <!-- Profile Info -->
        <div class="tw-card p-6 lg:col-span-2">
          <h2 class="text-lg font-semibold mb-4" style="color:var(--primary)">Profile Information</h2>
          <form method="POST" action="/admin/v1/profile/update?_method=PUT&_csrf=${_csrf}">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label class="form-label">[code] Code</label>
                <input type="text" name="code" value="${(old.code)!(user.code)!""}"
                       class="form-control <#if (errors.code)??>is-invalid</#if>"
                       placeholder="USR-001" required>
                <#if (errors.code)??><div class="invalid-feedback">${errors.code}</div></#if>
              </div>
              <div>
                <label class="form-label">[name] Full Name</label>
                <input type="text" name="name" value="${(old.name)!(user.name)!""}"
                       class="form-control <#if (errors.name)??>is-invalid</#if>" required>
                <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
              </div>
              <div>
                <label class="form-label">[phone] Phone</label>
                <input type="text" name="phone" value="${(old.phone)!(user.phone)!""}"
                       class="form-control <#if (errors.phone)??>is-invalid</#if>">
                <#if (errors.phone)??><div class="invalid-feedback">${errors.phone}</div></#if>
              </div>
              <div>
                <label class="form-label">[email] Email</label>
                <input type="email" name="email" value="${(old.email)!(user.email)!""}"
                       class="form-control <#if (errors.email)??>is-invalid</#if>" required>
                <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
              </div>
              <div class="md:col-span-2">
                <label class="form-label">[timezone] Timezone</label>
                <select name="timezone" class="form-control select2">
                  <#assign tzList = ["UTC","Asia/Jakarta","Asia/Makassar","Asia/Jayapura","Asia/Singapore","America/New_York","America/Los_Angeles","Europe/London","Europe/Paris"]>
                  <#list tzList as tz>
                    <option value="${tz}" <#if ((old.timezone)!(user.timezone)!"UTC") == tz>selected</#if>>${tz}</option>
                  </#list>
                </select>
              </div>
            </div>
            <div class="mt-4">
              <button type="submit" class="btn btn-primary"><i class="fas fa-save"></i> Update Profile</button>
            </div>
          </form>
        </div>

        <!-- Change Password -->
        <div class="tw-card p-6 lg:col-span-3">
          <h2 class="text-lg font-semibold mb-4" style="color:var(--primary)">Change Password</h2>
          <form method="POST" action="/admin/v1/profile/password?_csrf=${_csrf}" class="max-w-md">
            <div class="mb-4">
              <label class="form-label">Current Password</label>
              <input type="password" name="currentPassword"
                     class="form-control <#if (errors.currentPassword)??>is-invalid</#if>" required>
              <#if (errors.currentPassword)??><div class="invalid-feedback">${errors.currentPassword}</div></#if>
            </div>
            <div class="mb-4">
              <label class="form-label">New Password</label>
              <input type="password" name="newPassword"
                     class="form-control <#if (errors.newPassword)??>is-invalid</#if>" required>
              <#if (errors.newPassword)??><div class="invalid-feedback">${errors.newPassword}</div></#if>
            </div>
            <div class="mb-6">
              <label class="form-label">Confirm New Password</label>
              <input type="password" name="newPasswordConfirm"
                     class="form-control <#if (errors.newPasswordConfirm)??>is-invalid</#if>" required>
              <#if (errors.newPasswordConfirm)??><div class="invalid-feedback">${errors.newPasswordConfirm}</div></#if>
            </div>
            <button type="submit" class="btn btn-warning"><i class="fas fa-key"></i> Change Password</button>
          </form>
        </div>

      </div>
    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
</body>
</html>
