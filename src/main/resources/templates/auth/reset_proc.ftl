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
        <i class="fas fa-key fa-2x" style="color:var(--primary)"></i>
      </div>
      <h1 class="text-2xl font-bold" style="color:var(--primary)">Reset Password</h1>
      <p class="text-gray-500 text-sm mt-1">Enter Your New Password</p>
    </div>

    <#if flash??>
    <div class="alert alert-${flash.key} mb-4">
      <i class="fas fa-<#if flash.key == 'success'>check-circle<#else>exclamation-circle</#if>"></i>
      ${flash.message}
    </div>
    </#if>

    <form method="POST" action="/admin/v1/auth/reset/process?_csrf=${_csrf}">
      <div class="mb-4">
        <label class="form-label">Email Address</label>
        <input type="email" name="email" value="${(old.email)!""}"
               class="form-control <#if (errors.email)??>is-invalid</#if>"
               placeholder="your@email.com" required>
        <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
      </div>
      <div class="mb-4">
        <label class="form-label">OTP Code</label>
        <input type="text" name="otp" value="${(old.otp)!""}"
               class="form-control <#if (errors.otp)??>is-invalid</#if>"
               placeholder="Enter 6-digit OTP" maxlength="6" required>
        <#if (errors.otp)??><div class="invalid-feedback">${errors.otp}</div></#if>
      </div>
      <div class="mb-4">
        <label class="form-label">New Password</label>
        <input type="password" name="password"
               class="form-control <#if (errors.password)??>is-invalid</#if>"
               placeholder="Minimum 8 characters" required>
        <#if (errors.password)??><div class="invalid-feedback">${errors.password}</div></#if>
      </div>
      <div class="mb-6">
        <label class="form-label">Confirm New Password</label>
        <input type="password" name="passwordConfirm"
               class="form-control <#if (errors.passwordConfirm)??>is-invalid</#if>"
               placeholder="Repeat new password" required>
        <#if (errors.passwordConfirm)??><div class="invalid-feedback">${errors.passwordConfirm}</div></#if>
      </div>
      <button type="submit" class="btn btn-primary-tw w-100 py-2 mb-3">Reset Password</button>
      <p class="text-center text-sm text-gray-500">
        <a href="/admin/v1/auth/reset/req" class="text-primary-tw text-decoration-none">back?</a>
      </p>
    </form>
  </div>
  <#include "/layouts/foot.ftl">
</body>
</html>
