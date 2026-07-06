<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center p-4">
  <div class="w-full max-w-5xl tw-card overflow-hidden grid md:grid-cols-2">
    <#-- Left panel: login image bila di-set, fallback ikon + nama (paritas GoAdmin) -->
    <div class="hidden md:flex sidebar-gradient items-center justify-center p-10">
      <#if (setting.login_image)?has_content>
      <img src="${setting.login_image}" alt="Login"
           class="max-w-full max-h-80 object-contain">
      <#else>
      <div class="text-center text-white">
        <i class="fas fa-chart-line fa-4x mb-4 opacity-80"></i>
        <h2 class="text-2xl font-bold">${setting.name!"KotlinAdmin"}</h2>
      </div>
      </#if>
    </div>

    <#-- Right panel: login form -->
    <div class="p-8 flex flex-col justify-center">
      <div class="mb-6 text-center">
        <#if (setting.logo)?has_content>
        <img src="${setting.logo}" alt="logo"
             class="h-14 mx-auto object-contain mb-3">
        </#if>
        <h1 class="text-2xl font-bold" style="color:var(--primary)">Hello, Welcome Back!</h1>
        <p class="text-sm text-gray-500 mt-1">Enter your credentials to continue</p>
      </div>

      <#-- errorMessages array (validation errors shown as list) -->
      <#if (errors?has_content)>
      <div class="alert alert-danger mb-3">
        <ul class="mb-0 ps-3">
          <#list errors?keys as key>
          <li>${errors[key]}</li>
          </#list>
        </ul>
      </div>
      </#if>

      <#-- Single flash message -->
      <#if flash??>
      <div class="alert alert-${flash.key} mb-3">
        <span>${flash.message}</span>
      </div>
      </#if>

      <form method="POST" action="/auth/login?_csrf=${_csrf}">
        <div class="mb-3">
          <label class="form-label">Email Address</label>
          <input type="email" name="email" value="${(old.email)!""}"
                 class="form-control <#if (errors.email)??>is-invalid</#if>"
                 placeholder="Email address">
          <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
        </div>
        <div class="mb-3">
          <label class="form-label">Password</label>
          <input type="password" name="password"
                 class="form-control <#if (errors.password)??>is-invalid</#if>"
                 placeholder="Password">
          <#if (errors.password)??><div class="invalid-feedback">${errors.password}</div></#if>
        </div>
        <button type="submit" class="btn btn-primary-tw w-100 py-2 mb-3">Login</button>
        <div class="d-flex justify-content-between small mb-3">
          <div class="form-check">
            <input type="checkbox" class="form-check-input" id="remember" name="remember">
            <label class="form-check-label" for="remember">Keep me logged in</label>
          </div>
          <a href="/admin/v1/auth/reset/req" class="text-primary-tw text-decoration-none">Forgot password</a>
        </div>
        <hr class="my-4">
        <p class="text-center text-sm text-gray-500">
          Don't have an account?
          <a href="/auth/register" class="text-primary-tw text-decoration-none fw-semibold">create here</a>
        </p>
      </form>
    </div>
  </div>
  <#include "/layouts/foot.ftl">
</body>
</html>
