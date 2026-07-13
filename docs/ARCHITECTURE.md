# Arsitektur — KotlinAdmin

Dokumen ini menjelaskan struktur, lapisan, dan keputusan desain KotlinAdmin. Ditujukan untuk developer yang akan mengembangkan atau menambah fitur.

## Gambaran Umum

KotlinAdmin adalah port Kotlin/Ktor dari NodeAdmin. Ktor sangat **un-opinionated** — setiap fitur harus di-`install()` eksplisit, tidak ada magic auto-configuration. Aplikasi **modular per fitur**: setiap modul (`modules/{modul}/`) berdiri sendiri dengan lapisannya, dan didaftarkan sebagai Ktor Application extension function.

```
HTTP Request
  → Ktor Plugin pipeline (urutan install = urutan eksekusi)
      MethodOverridePlugin   ← POST+?_method=PUT/DELETE → atribut EffectiveMethod
      CsrfPlugin             ← validasi token pada POST/PUT/PATCH/DELETE
      SecurityHeaders        ← inject HSTS/X-Frame-Options/CSP
      RateLimit              ← per-IP throttle pada endpoint sensitif
      Sessions               ← baca/tulis UserSession (Redis store)
      Authentication         ← JWT validator untuk /api/* routes
      StatusPages            ← tangkap AppException → HTTP response
      Routing                ← dispatch ke route handler
  → requireAuthenticated()   ← cek session, redirect /auth/login jika belum
  → checkAccess(name, method)← RBAC: RouteRegistry reverse-lookup → HasAccess
  → Handler (tipis)          ← parse params/body, panggil service, respondView/redirect
  → Service (I*Service)      ← logika bisnis, dbQuery, throw AppException
  → dbQuery { }              ← newSuspendedTransaction(Dispatchers.IO)
  → Exposed Entity / DB
  ↘ AppException → StatusPages
```

---

## Lapisan

| Lapisan | Lokasi | Tanggung Jawab |
|---------|--------|----------------|
| Plugin | `core/plugins/` | Cross-cutting concerns: CSRF, method-override, security headers |
| Route | `modules/*/routes/*.kt` | URL binding, middleware chain, delegate ke service |
| Service | `modules/*/services/*.kt` | Logika bisnis. Implements `I*Service`. `throw AppException`. |
| DB Query | `dbQuery { }` wrapper | `newSuspendedTransaction` — semua Exposed ops di sini |
| Entity | `modules/*/models/*Table.kt` | `IdTable<String>` + `Entity<String>` — schema ORM |
| Migration | `resources/db/migrations/*.sql` | Flyway versioned SQL portabel |
| Template | `resources/templates/` | FreeMarker `.ftl` + `<#ftl output_format="HTML">` |
| DI | `di/AppModule.kt` | Koin module: `single<I*Service> { *Service() }` |

---

## Plugin Install Order (KRITIS)

Urutan `install()` di `Application.kt` = urutan eksekusi pipeline. Perubahan urutan = bug subtle.

```kotlin
fun Application.configurePlugins(config: AppConfig) {
    install(CORS) { ... }
    install(Compression) { gzip() }
    install(MethodOverridePlugin)      // ← WAJIB sebelum Routing
    install(RateLimit) { ... }
    install(StatusPages) { ... }
    install(ContentNegotiation) { json() }

    if (config.appMode == "full") {
        install(Sessions) { ... }      // ← sebelum CSRF (CSRF baca session)
        install(CsrfPlugin)            // ← sebelum route handler
        install(SecurityHeadersPlugin)
        install(FreeMarker) { ... }
        staticResources("/assets", "static")
    }

    install(Authentication) {
        jwt("api") { ... }
    }
    // Routing diinstall terakhir via routing { } block
}
```

---

## RBAC Route-Driven

Model otorisasi **bukan subject-based** (`user.delete`), melainkan **diturunkan dari route**:

- **Permission = `(name, method, guard_name)`** — `name` = nama named-route (`admin.v1.access.user.delete`), `method` = HTTP method, `guard_name` = `api` bila nama berawalan `api.`, selainnya `web`.
- **RouteRegistry** — singleton yang menyimpan semua `(name, method, path)` yang didaftarkan via `namedGet`/`namedPost`/`namedPut`/`namedDelete` DSL saat aplikasi boot.
- **Auto-sync** — `PermissionService.syncFromRouteRegistry()` memindai seluruh RouteRegistry dan meng-upsert permission yang belum ada (idempoten). Dipicu lazy saat halaman Permission dibuka.
- **`checkAccess(name, method)`** — fungsi di setiap handler admin: reverse-lookup `(path, method)` → `name` → cek apakah salah satu role user punya permission dengan `name` DAN `method` yang cocok.
- **Administrator bypass** — role dengan nama `Administrator` melewati semua pengecekan.
- **Sidebar gating** — `hasAccess(session, "admin.v1.access.user.index", "GET")` di FreeMarker sidebar.

### Named Route DSL

```kotlin
// core/routing/RouteDsl.kt
fun Route.namedGet(name: String, path: String = "", handler: ...) {
    RouteRegistry.register(name, "GET", resolvePath(path))
    if (path.isEmpty()) get { handler() } else get(path) { handler() }
}

fun Route.namedPut(name: String, path: String = "", handler: ...) {
    RouteRegistry.register(name, "PUT", resolvePath(path))
    // Handles real PUT (API) AND POST+?_method=PUT (web form)
    route(path) {
        put { handler() }
        post {
            val override = call.request.queryParameters["_method"]?.uppercase()
            if (override == "PUT") handler() else call.respond(HttpStatusCode.MethodNotAllowed)
        }
    }
}
// namedDelete analog — POST+?_method=DELETE
```

---

## Dependency Injection (Koin)

```kotlin
// di/AppModule.kt
fun appModule(config: AppConfig) = module {
    single { config }
    single<IUserService>     { UserService() }
    single<IRoleService>     { RoleService() }
    single<IPermissionService> { PermissionService() }
    single<IAuthService>     { AuthService(get()) }
    single<ISettingService>  { SettingService() }
    single<IDashboardService>{ DashboardService() }
    single<IProfileService>  { ProfileService() }
}

// Dalam Application extension function:
fun Application.userModule() {
    val userService = get<IUserService>()  // resolve dari Koin
    routing {
        route("/admin/v1/access/user") {
            namedGet("admin.v1.access.user.index") {
                val session = call.requireAuthenticated()
                val result = userService.index(call.request.queryParameters)
                call.respondView("access/users/index.ftl", mapOf("datas" to result.items, ...))
            }
        }
    }
}
```

---

## Error Handling

```kotlin
// core/errors/AppException.kt
sealed class AppException(msg: String, val statusCode: HttpStatusCode) : RuntimeException(msg)
class NotFoundError(msg: String = "Not Found")              : AppException(msg, HttpStatusCode.NotFound)
class ConflictError(msg: String = "Conflict")               : AppException(msg, HttpStatusCode.Conflict)
class ValidationError(msg: String, val fields: Map<String, String> = emptyMap())
                                                             : AppException(msg, HttpStatusCode.UnprocessableEntity)
class UnauthorizedError(msg: String = "Unauthorized")        : AppException(msg, HttpStatusCode.Unauthorized)
class ForbiddenError(msg: String = "Forbidden")              : AppException(msg, HttpStatusCode.Forbidden)

// Application.kt — StatusPages terpusat
install(StatusPages) {
    exception<AppException> { call, ex ->
        val isApi = call.request.path().startsWith("/api/")
        when {
            ex is UnauthorizedError && !isApi -> call.respondRedirect("/auth/login")
            ex is ForbiddenError   && !isApi -> call.respondRedirect("/admin/v1/dashboard")
            isApi -> call.respond(ex.statusCode, mapOf("status" to "error", "message" to ex.message))
            else  -> {
                val session = call.sessions.get<UserSession>()
                if (session != null) call.sessions.set(session.withFlash("danger", ex.message ?: "Error"))
                call.respondRedirect(call.request.headers["Referer"] ?: "/admin/v1/dashboard")
            }
        }
    }
}
```

---

## Konfigurasi (HOCON + AppConfig)

```hocon
# src/main/resources/application.conf
ktor.deployment.port = 8080

app {
  name = "KotlinAdmin"
  mode = "full"          # full | api
  jwtSecret = ${JWT_SECRET}
  jwtExpireHours = 24
  sessionSecret = ${SESSION_SECRET}
  bcryptRounds = 12
}

database {
  # DB_TYPE menentukan engine: sqlite (default) | mysql | postgres.
  # URL & driver JDBC diturunkan di AppConfig.resolveDb dari type + host/port/name.
  type     = ${?DB_TYPE}      # sqlite
  host     = ${?DB_HOST}      # localhost
  port     = ${?DB_PORT}      # 3306 (mysql) / 5432 (postgres)
  name     = ${?DB_DATABASE}  # kotlinadmin
  user     = ${?DB_USERNAME}
  password = ${?DB_PASSWORD}

  # Override penuh — dipakai hanya bila butuh parameter JDBC khusus (mis. sslmode).
  url    = ${?DB_URL}
  driver = ${?DB_DRIVER}
}

redis {
  host = ${?REDIS_HOST}    # localhost
  port = ${?REDIS_PORT}    # 6379
}
```

`AppConfig` divalidasi saat startup — secret kosong di produksi → `error("JWT_SECRET wajib diisi")`.

---

## Session & Auth

### Web Session (Cookie)
- Plugin: `install(Sessions) { cookie<UserSession>("SESSION") { ... } }`
- Store: `RedisSessionStorage` (Lettuce) — stateless, horizontal scaling ready
- `UserSession` membawa: `userId`, `userName`, `userEmail`, `roles`, `flash`, `errors`, `old`

### JWT (API)
- Plugin: `install(Authentication) { jwt("api") { ... } }`
- Blacklist: Redis `SETEX jti ttl "1"` saat logout
- Verifikasi: signature + exp + not-in-blacklist
- Endpoint API menggunakan `authenticate("api") { ... }` block di routing

---

## DB Layer

### Entity (Exposed DAO)
```kotlin
// modules/access/models/Tables.kt
object Users : IdTable<String>("users") {
    override val id = varchar("id", 36).entityId()
    override val primaryKey = PrimaryKey(id)
    val code    = varchar("code", 20).uniqueIndex()
    val name    = varchar("name", 50)
    val email   = varchar("email", 255).uniqueIndex()
    val status  = varchar("status", 20).default("Active")
    // ... (semua kolom kanonik)
}

class UserEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, UserEntity>(Users)
    var code   by Users.code
    var name   by Users.name
    var email  by Users.email
    // ...
}
```

Kolom `desc` di Roles/Permissions: property Kotlin bernama `description`, DB column `"desc"` — Exposed auto-quote per dialek.

### Join Tables (PIN Manual)
```kotlin
object UsersRoles : Table("users_roles") {
    val userId = varchar("user_id", 36) references Users.id
    val roleId = varchar("role_id", 36) references Roles.id
    override val primaryKey = PrimaryKey(userId, roleId)
}
```

### dbQuery Wrapper
```kotlin
// config/DatabaseConfig.kt
suspend fun <T> dbQuery(block: Transaction.() -> T): T =
    newSuspendedTransaction(Dispatchers.IO, block = block)
```

### Migration (Flyway)
- SQL plain di `resources/db/migrations/V{N}__{CamelCase}.sql`
- Dev: auto-apply saat startup (`Flyway.configure().load().migrate()`)
- Produksi: migrasi eksplisit (`./gradlew flywayMigrate` atau via deployment pipeline)

---

## Tema Admin (Switchable)

9 tema (Blue default) disimpan di DB `settings.theme`. Setiap `respondView` inject:
```kotlin
mapOf(
    "theme"     to currentTheme.palette,   // Map(primary, secondary, light, dark)
    "themeName" to currentTheme.name,
    "themes"    to ALL_THEMES,
    "setting"   to settingMap,
    "user"      to sessionUserMap,
    "flash"     to session.flash,
    "errors"    to session.errors,
    "old"       to session.old,
    "_csrf"     to session.csrfToken
)
```

FreeMarker `head.ftl` memetakan ke CSS vars:
```html
<style>:root {
  --primary: ${theme.primary};
  --secondary: ${theme.secondary};
  --theme-light: ${theme.light};
  --theme-dark: ${theme.dark};
}</style>
```

---

## Varian APP_MODE

| Mode | Yang diinstall | Yang dilewati |
|------|---------------|---------------|
| `full` | Sessions, CSRF, FreeMarker, static admin, route web | — |
| `api` | Authentication JWT, ContentNegotiation JSON | Sessions, CSRF, FreeMarker, static admin, route web |

Guard di `Application.kt`:
```kotlin
if (config.appMode == "full") {
    install(Sessions) { ... }
    install(CsrfPlugin)
    install(FreeMarker) { ... }
    staticResources("/assets", "static")
    // register web modules
}
// API routes selalu terdaftar
```

Upgrade API-only → Full: `./gradlew addUi`

---

## Helper DRY

| Helper | Lokasi | Guna |
|--------|--------|------|
| `dbQuery { }` | `config/DatabaseConfig.kt` | `newSuspendedTransaction` wrapper |
| `paginate(list, page, size)` | `core/helpers/PaginationHelper.kt` | pagination seragam |
| `ciLike(value)` | `core/helpers/QueryHelper.kt` | LIKE case-insensitive lintas-dialek |
| `generateOtp()` / `verifyOtp()` | `core/helpers/OtpHelper.kt` | OTP aman (SecureRandom + hash) |
| `call.respondView(template, model)` | `core/helpers/ViewHelper.kt` | render FTL + inject locals |
| `call.requireAuthenticated()` | `core/routing/AuthHelper.kt` | cek session atau redirect login |
| `call.checkAccess(name, method)` | `core/routing/AuthHelper.kt` | RBAC check atau throw ForbiddenError |

---

## Storage & switching backends

Adapter penyimpanan file — mirror desain `FileService` NodeAdmin. **DB menyimpan
_key_ object** (mis. `media/1720000000_logo.png`), **URL render dibangun saat
request** via `IStorageService.url(key)`. Berpindah backend cukup ubah `.env` +
restart — **tanpa** ubah kode atau template.

**Komponen** (`core/storage/`):

| Berkas | Peran |
|--------|-------|
| `IStorageService` | kontrak: `put` / `url` / `list` / `delete` / `localMount` |
| `LocalStorageService` | driver filesystem lokal |
| `ObjectStorageService` | driver `oss` (Alibaba) & `s3` (AWS / S3-compatible), tanpa SDK — signing manual (S3 SigV4, OSS V1) via `java.net.http.HttpClient` |

Di-inject Koin (`di/AppModule.kt`) berdasar `STORAGE_DRIVER`. `MediaService` dan
helper template `getFile(key)` (`ViewHelper.storageUrl`) memakainya; keduanya
tidak tahu driver mana yang aktif.

**URL render per driver** (`url(key)`):

| `STORAGE_DRIVER` | URL yang dirender | Penyajian |
|------------------|-------------------|-----------|
| `local` (default) | `/storage/<key>` (path relatif, prefix stabil) | route static `staticFiles("/storage", baseDir)` didaftarkan **hanya** saat driver=local (`Application.configureRouting`) |
| `s3` | presigned URL absolut (SigV4, TTL 6 jam) | langsung dari bucket (privat) |
| `oss` | URL publik virtual-hosted absolut `https://<bucket>.<endpoint>/<key>` | langsung dari OSS |

Prefix URL `/storage` **dipisah** dari path filesystem: `STORAGE_BASE_PATH` boleh
absolut (mis. `/data/uploads` di Docker) namun URL tetap `/storage/<key>` yang
valid — bukan `//data/uploads/...`.

**Contoh `.env`:**

```env
# Lokal (default) — tanpa kredensial
STORAGE_DRIVER=local
STORAGE_BASE_PATH=uploads

# AWS S3 / S3-compatible (MinIO, R2, B2)
STORAGE_DRIVER=s3
STORAGE_ACCESS_KEY_ID=...
STORAGE_SECRET_ACCESS_KEY=...
STORAGE_BUCKET=my-bucket
STORAGE_REGION=ap-southeast-1
STORAGE_ENDPOINT=            # kosong = AWS; isi = path-style (MinIO/R2)

# Alibaba OSS
STORAGE_DRIVER=oss
STORAGE_ACCESS_KEY_ID=...
STORAGE_SECRET_ACCESS_KEY=...
STORAGE_BUCKET=my-bucket
STORAGE_ENDPOINT=oss-ap-southeast-5.aliyuncs.com
```

**Catatan operasional:**

- **DB menyimpan key, bukan URL.** Berpindah driver tidak butuh migrasi data —
  URL dibangun ulang saat render. (URL `http(s)://` absolut lama diteruskan
  apa adanya oleh `getFile` demi kompatibilitas.)
- **Migrasi konten** saat pindah backend: salin object apa adanya, key dipertahankan.
  - ke S3: `aws s3 sync ./uploads s3://my-bucket/`
  - ke OSS: `ossutil cp -r ./uploads oss://my-bucket/`
- **Local di produksi bersifat ephemeral** (hilang saat kontainer di-recreate).
  Untuk driver=local di produksi, mount **persistent volume** ke `STORAGE_BASE_PATH`
  (mis. `-v /data/uploads:/app/uploads`), atau gunakan `s3`/`oss`.
- **Konten unggahan di-git-ignore** (`/uploads/*`, `/storage/*`); folder dijaga
  via `.gitkeep`.
