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

      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-bold text-gray-800">Profile</h1>
      </div>

      <div class="tw-card p-6">
        <h2 class="text-lg font-bold mb-4" style="color:var(--primary)">User Form</h2>
        <form method="POST" action="/admin/v1/profile/update?_method=PUT&_csrf=${_csrf}" enctype="multipart/form-data">
          <div class="mb-3">
            <label for="code" class="form-label fw-semibold">Code</label>
            <input id="code" type="text" class="form-control <#if (errors.code)??>is-invalid</#if>" name="code" value="${(old.code)!(user.code)!""}">
            <#if (errors.code)??><div class="invalid-feedback">${errors.code}</div></#if>
          </div>
          <div class="mb-3">
            <label for="name" class="form-label fw-semibold">Name</label>
            <input id="name" type="text" class="form-control <#if (errors.name)??>is-invalid</#if>" name="name" value="${(old.name)!(user.name)!""}">
            <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
          </div>
          <div class="mb-3">
            <label for="phone" class="form-label fw-semibold">Phone Number</label>
            <input id="phone" type="text" class="form-control <#if (errors.phone)??>is-invalid</#if>" name="phone" value="${(old.phone)!(user.phone)!""}">
            <#if (errors.phone)??><div class="invalid-feedback">${errors.phone}</div></#if>
          </div>
          <div class="mb-3">
            <label for="email" class="form-label fw-semibold">Email</label>
            <input id="email" type="email" class="form-control <#if (errors.email)??>is-invalid</#if>" name="email" value="${(old.email)!(user.email)!""}">
            <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
          </div>
          <div class="mb-3">
            <label for="timezone" class="form-label fw-semibold">Timezone</label>
            <select id="timezone" name="timezone" class="form-control <#if (errors.timezone)??>is-invalid</#if>">
              <#list timezones as tz>
                <option value="${tz}" <#if ((old.timezone)!(user.timezone)!"") == tz>selected</#if>>${tz}</option>
              </#list>
            </select>
            <#if (errors.timezone)??><div class="invalid-feedback">${errors.timezone}</div></#if>
          </div>
          <div class="mb-3">
            <label for="password" class="form-label fw-semibold">Password</label>
            <input id="password" type="password" class="form-control <#if (errors.password)??>is-invalid</#if>" name="password" value="" autocomplete="off">
            <#if (errors.password)??><div class="invalid-feedback">${errors.password}</div></#if>
          </div>
          <div class="mb-3">
            <label for="password_confirmation" class="form-label fw-semibold">Password Confirm</label>
            <input id="password_confirmation" type="password" class="form-control <#if (errors.password_confirmation)??>is-invalid</#if>" name="password_confirmation" value="">
            <#if (errors.password_confirmation)??><div class="invalid-feedback">${errors.password_confirmation}</div></#if>
          </div>
          <div class="mb-3">
            <label for="status" class="form-label fw-semibold">Status</label>
            <select name="status" id="status" class="form-control <#if (errors.status)??>is-invalid</#if>" required>
              <option value="Active" <#if ((old.status)!(user.status)!"") == "Active">selected</#if>>Active</option>
              <option value="Inactive" <#if ((old.status)!(user.status)!"") == "Inactive">selected</#if>>Inactive</option>
            </select>
            <#if (errors.status)??><div class="invalid-feedback">${errors.status}</div></#if>
          </div>
          <div class="mb-4">
            <label for="picture" class="form-label fw-semibold">Picture</label>
            <div class="d-flex align-items-center gap-3">
              <img id="picturePreview" src="${getFile((user.picture)!'modules/access/user/user.png')}" width="90" height="90"
                   class="rounded border p-1" style="object-fit:contain;background:#f8fafc"
                   onerror="this.style.visibility='hidden'">
              <input id="picture" type="file" name="picture" accept="image/*" class="form-control <#if (errors.picture)??>is-invalid</#if>">
            </div>
            <#if (errors.picture)??><div class="text-danger small mt-1">${errors.picture}</div></#if>
          </div>
          <button type="submit" class="btn btn-primary-tw px-4 py-2"><i class="fas fa-save me-1"></i> Save</button>
        </form>
      </div>

      <script>
        // Preview gambar sebelum submit (sama dengan File Upload di UI Components).
        (function () {
          var input = document.getElementById('picture'), prev = document.getElementById('picturePreview');
          if (!input || !prev) return;
          input.addEventListener('change', function () {
            var f = this.files && this.files[0];
            if (f && f.type.startsWith('image/')) { prev.src = URL.createObjectURL(f); prev.style.visibility = 'visible'; }
          });
        })();
      </script>

    </main>
  </div>
</div>
<#include "/layouts/foot.ftl">
</body>
</html>
