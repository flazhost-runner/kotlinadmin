<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/layouts/head.ftl">
  <title>${setting.name!"KotlinAdmin"} - Register</title>
</head>
<body class="bg-gray-100 min-h-screen flex items-center justify-center p-4">
  <div class="tw-card max-w-md w-full p-8">
    <div class="mb-6 text-center">
      <img src="${(setting.logo)!""}" alt="logo" width="48" height="48" class="mx-auto mb-3 rounded-lg">
      <h1 class="text-2xl font-bold text-gray-800">Create Account</h1>
      <p class="text-gray-500 text-sm mt-1">Fill in the details below to register</p>
    </div>

    <#if flash??>
    <div class="alert alert-${flash.key} mb-4">
      <i class="fas fa-exclamation-circle"></i> ${flash.message}
    </div>
    </#if>

    <form method="POST" action="/auth/register?_csrf=${_csrf}">
      <div class="mb-4">
        <label class="form-label">Full Name</label>
        <input type="text" name="name" value="${(old.name)!""}"
               class="form-control <#if (errors.name)??>is-invalid</#if>"
               placeholder="John Doe" required>
        <#if (errors.name)??><div class="invalid-feedback">${errors.name}</div></#if>
      </div>
      <div class="mb-4">
        <label class="form-label">Email Address</label>
        <input type="email" name="email" value="${(old.email)!""}"
               class="form-control <#if (errors.email)??>is-invalid</#if>"
               placeholder="you@example.com" required>
        <#if (errors.email)??><div class="invalid-feedback">${errors.email}</div></#if>
      </div>
      <div class="mb-4">
        <label class="form-label">Password</label>
        <input type="password" name="password"
               class="form-control <#if (errors.password)??>is-invalid</#if>"
               placeholder="Minimum 8 characters" required>
        <#if (errors.password)??><div class="invalid-feedback">${errors.password}</div></#if>
      </div>
      <div class="mb-6">
        <label class="form-label">Confirm Password</label>
        <input type="password" name="passwordConfirm"
               class="form-control <#if (errors.passwordConfirm)??>is-invalid</#if>"
               placeholder="Repeat password" required>
        <#if (errors.passwordConfirm)??><div class="invalid-feedback">${errors.passwordConfirm}</div></#if>
      </div>
      <button type="submit" class="btn btn-primary w-full py-2 mb-4">
        <i class="fas fa-user-plus"></i> Create Account
      </button>
      <hr class="border-gray-200 mb-4">
      <p class="text-center text-sm text-gray-500">
        Already have an account?
        <a href="/auth/login" class="text-[color:var(--primary)] font-semibold hover:underline">login here</a>
      </p>
    </form>
  </div>
  <#include "/layouts/foot.ftl">
</body>
</html>
