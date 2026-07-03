<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Reset Password</title>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center p-4">
  <div class="tw-card max-w-md w-full p-8">
    <div class="mb-6 text-center">
      <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-3"
           style="background:var(--theme-light)">
        <i class="fas fa-lock-open fa-2x" style="color:var(--primary)"></i>
      </div>
      <h1 class="text-2xl font-bold" style="color:var(--primary)">Forgot Password</h1>
      <p class="text-gray-500 text-sm mt-1">Enter your Email to continue</p>
    </div>

    <#if flash??>
    <div class="alert alert-${flash.key} mb-4">
      <i class="fas fa-<#if flash.key == 'success'>check-circle<#else>exclamation-circle</#if>"></i>
      ${flash.message}
    </div>
    </#if>

    <form method="POST" action="/admin/v1/auth/reset/request?_csrf=${_csrf}">
      <div class="mb-6">
        <label class="form-label">Email Address</label>
        <input type="email" name="email" value="${(old.email)!""}"
               class="form-control <#if (errors.email)??>is-invalid</#if>"
               placeholder="Email address">
        <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
      </div>
      <button type="submit" class="btn btn-primary-tw w-100 py-2 mb-3">Send OTP</button>
      <p class="text-center text-sm text-gray-500">
        <a href="/auth/login" class="text-primary-tw text-decoration-none">back?</a>
      </p>
    </form>
  </div>
  <#include "/layouts/foot.ftl">
</body>
</html>
