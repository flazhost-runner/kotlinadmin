<#ftl output_format="HTML">
<!DOCTYPE html>
<html lang="en">
<head>
  <#include "/home/head_fe.ftl">
  <title>${setting.name!"KotlinAdmin"}</title>
</head>
<body class="bg-gray-50 text-gray-800">

<!-- Navbar -->
<nav class="bg-white shadow-sm sticky top-0 z-50">
  <div class="max-w-6xl mx-auto px-4 py-3 flex items-center justify-between">
    <a href="/" class="flex items-center gap-2 font-bold text-xl" style="color:var(--primary)">
      <img src="${setting.logo!""}" alt="${setting.name!""}" width="32" height="32" class="rounded"
           onerror="this.style.display='none'">
      ${setting.name!"KotlinAdmin"}
    </a>
    <div class="flex items-center gap-4">
      <a href="/login" class="text-gray-600 hover:text-primary text-sm font-medium">Login</a>
      <a href="/register" class="px-4 py-2 rounded-lg text-white text-sm font-medium"
         style="background:var(--primary)">Get Started</a>
    </div>
  </div>
</nav>

<!-- Hero -->
<section class="py-20 text-center">
  <div class="max-w-4xl mx-auto px-4">
    <div class="inline-flex items-center gap-2 bg-blue-50 text-blue-700 text-xs font-semibold px-3 py-1 rounded-full mb-6">
      <i class="fas fa-rocket"></i> Powered by Kotlin + Ktor
    </div>
    <h1 class="text-5xl font-bold mb-6 leading-tight">
      ${setting.tagline!"The Modern Admin Panel"}
    </h1>
    <p class="text-xl text-gray-500 mb-10 max-w-2xl mx-auto">
      ${setting.description!"A full-featured admin panel built with Kotlin/Ktor, Exposed, Koin DI, FreeMarker, and Flyway."}
    </p>
    <div class="flex justify-center gap-4">
      <a href="/login" class="px-8 py-3 rounded-xl text-white font-semibold shadow-md hover:opacity-90 transition"
         style="background:var(--primary)">
        <i class="fas fa-sign-in-alt mr-2"></i> Sign In
      </a>
      <a href="/register" class="px-8 py-3 rounded-xl border-2 font-semibold hover:bg-gray-50 transition"
         style="border-color:var(--primary);color:var(--primary)">
        <i class="fas fa-user-plus mr-2"></i> Register
      </a>
    </div>
  </div>
</section>

<!-- Features -->
<section class="py-16 bg-white">
  <div class="max-w-6xl mx-auto px-4">
    <h2 class="text-3xl font-bold text-center mb-12">Built for Production</h2>
    <div class="grid grid-cols-1 md:grid-cols-3 gap-8">
      <#assign features = [
        {"icon": "fa-shield-halved", "title": "RBAC Security", "desc": "Route-driven permissions with role-based access control, CSRF protection, and session management."},
        {"icon": "fa-database", "title": "Exposed DAO + Flyway", "desc": "Type-safe SQL with Exposed ORM and versioned schema migrations via Flyway."},
        {"icon": "fa-puzzle-piece", "title": "Modular & Extensible", "desc": "DI with Koin, APP_MODE=full|api, makeModule task, and clear module conventions."},
        {"icon": "fa-paint-brush", "title": "9 Themes", "desc": "DB-driven CSS variables with Tailwind CDN. Switch themes instantly without rebuild."},
        {"icon": "fa-bolt", "title": "Ktor 3.x Async", "desc": "Non-blocking coroutine-based server with Netty for high throughput."},
        {"icon": "fa-vial", "title": "Test Ready", "desc": "Kotest + Cucumber BDD, Detekt linting, and a convention checker for code quality."}
      ]>
      <#list features as f>
      <div class="p-6 rounded-xl border border-gray-100 shadow-sm hover:shadow-md transition">
        <div class="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style="background:var(--primary)20">
          <i class="fas ${f.icon} text-2xl" style="color:var(--primary)"></i>
        </div>
        <h3 class="font-semibold text-lg mb-2">${f.title}</h3>
        <p class="text-gray-500 text-sm">${f.desc}</p>
      </div>
      </#list>
    </div>
  </div>
</section>

<!-- Tech Stack -->
<section class="py-16">
  <div class="max-w-6xl mx-auto px-4 text-center">
    <h2 class="text-2xl font-bold mb-8 text-gray-500">Built With</h2>
    <div class="flex flex-wrap justify-center gap-6 text-gray-400">
      <#assign stack = ["Kotlin 2.x", "Ktor 3.x", "Exposed ORM", "Flyway", "Koin DI", "FreeMarker", "Kotest", "Detekt"]>
      <#list stack as s>
        <span class="px-4 py-2 bg-white rounded-lg border text-sm font-medium">${s}</span>
      </#list>
    </div>
  </div>
</section>

<!-- Footer -->
<footer class="py-8 bg-white border-t text-center text-gray-400 text-sm">
  <p>&copy; ${.now?string("yyyy")} ${setting.name!"KotlinAdmin"} &mdash; ${setting.tagline!"Built with Kotlin"}</p>
</footer>

</body>
</html>
