# AGENTS.md — Aturan Pengembangan KotlinAdmin (untuk AI & developer)

> **Sumber kebenaran tunggal.** Setiap AI (Claude Code, Cursor, Copilot) dan developer WAJIB mengikuti dokumen ini saat menambah/mengubah kode. Penyimpangan **ditolak CI** via `./gradlew check` (Detekt custom rules + test).

KotlinAdmin adalah port Kotlin/Ktor dari NodeAdmin (Node.js/Express/TypeScript). Yang diportasi adalah **konsep, prinsip, dan alur** — bukan kode mentah.

Sebelum coding, baca juga: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md), [`docs/MODULE_GUIDE.md`](docs/MODULE_GUIDE.md), [`docs/TESTING.md`](docs/TESTING.md).

---

## Alur Wajib (request lifecycle)

```
Route (namedGet/namedPost/namedPut/namedDelete DSL)
  → requireAuthenticated()              [periksa session, redirect login jika belum]
  → checkAccess(routeName, method)      [RBAC: cek permission; Administrator bypass]
  → handler tipis (parse params/body → call service → respondView / respondRedirect)
  → Service (implements I*Service)      [logika bisnis; throw AppException bila gagal]
  → dbQuery { ... }                     [semua akses Exposed dalam newSuspendedTransaction]
  → Exposed Entity / DB
  ↘ AppException apa pun → StatusPages (terpusat, petakan ke HTTP/redirect)
```

## Prinsip Wajib

1. **SOLID / DI (Koin).** Service `implements I*Service`; di-register Koin (`single<IUserService> { UserService() }`); route/handler minta via `get<I*Service>()`. **DILARANG** `UserService()` atau `new` langsung di route handler.
2. **Error terpusat.** Service **`throw AppException`** (`NotFoundError` / `ConflictError` / `ValidationError` / `UnauthorizedError` / `ForbiddenError`). Handler TIDAK menangkap error bisnis. **DILARANG** `return error`, `catch(AppException)` per-handler, atau `if (result == null) respond(404)`.
3. **Separation of Concerns.** Handler (HTTP parsing + respond) ≠ Service (logika bisnis) ≠ `dbQuery` (data). Logika bisnis hanya di service.
4. **Config terpusat.** Env hanya via `AppConfig` (diinject Koin). **DILARANG** `System.getenv(...)` atau `System.getProperty(...)` di `modules/`.
5. **Transaction wajib.** SEMUA operasi Exposed dalam `dbQuery { }` (= `newSuspendedTransaction(Dispatchers.IO)`). Akses entity di luar transaction = `LazyInitializationException`.
6. **Portabilitas DB.** Tipe kolom abstrak: `varchar`, `text`, `long`, `bool`. Kolom `id` = `varchar(36)` UUID string via `IdTable<String>` (**BUKAN** `UUIDTable` yang pakai UUID native). Join table di-PIN manual (`object UsersRoles : Table("users_roles")`).
7. **Plugin order kritis.** `MethodOverridePlugin` install **sebelum** `Routing`. `CsrfPlugin` install sebelum route handler. Lihat `Application.kt` untuk urutan yang benar.
8. **Session sebelum receiveMultipart.** Baca/set session **sebelum** `call.receiveMultipart()` — body habis setelah receive pertama.
9. **FreeMarker auto-escape.** Setiap template wajib `<#ftl output_format="HTML">` di baris pertama. Gunakan `${varName}` (auto-escaped), `${varName?no_esc}` hanya untuk HTML yang sudah disanitasi server.

---

## Sebelum Coding: Sajikan Rencana Artefak + Konfirmasi

Saat diminta membuat fitur/modul, AI **wajib** menyimpulkan artefak yang dibutuhkan (pakai Matriks di bawah) lalu **menyajikan rencana**. Ajukan pertanyaan hanya bila ambigu:
- Butuh **UI admin** (halaman web) atau **API-only**?
- **Read-only** atau **CRUD** (ada input tulis)?
- Butuh endpoint **API** atau cukup web?

## Matriks Kebutuhan Artefak

**TEST WAJIB untuk fitur APA PUN.** Setiap modul yang terjangkau lewat route harus punya minimal 1 test.

**Selalu ada** (modul fungsional ber-service):

| Artefak | Catatan |
|---------|---------|
| Service + `I*Service` interface | semua logika bisnis |
| Route file (`*Routes.kt`) | handler HTTP tipis |
| **Test** | **WAJIB** — ≥1 test/modul; integration jika ada service; BDD jika user-facing |
| Update docs | README; + `docs/API.md` bila ada API |

**Kondisional** (sesuai kebutuhan):

| Artefak | Wajib JIKA |
|---------|------------|
| `IdTable<String>` entity | menyimpan data |
| Flyway SQL migration | **ada entity** → migration wajib |
| DTO data class | **ada input tulis** (store/update) |
| FreeMarker templates | ada **UI admin** |
| API routes (terpisah) | fitur perlu API |

---

## Checklist Membuat Modul Baru

Jalankan `./gradlew makeModule -Pmodule=NamaModul` atau ikuti manual (lihat `docs/MODULE_GUIDE.md`):

1. **Entity** `modules/{m}/models/{M}Table.kt` — `IdTable<String>("tableName")`, tipe kolom abstrak, id varchar(36)
2. **Migration** `resources/db/migrations/V{N}__{CamelCase}.sql` — SQL portabel (hindari tipe vendor spesifik)
3. **Interface** `modules/{m}/services/I{M}Service.kt`
4. **Service** `modules/{m}/services/{M}Service.kt` — `class {M}Service : I{M}Service`, gunakan `dbQuery { }`, `throw AppException`
5. **Register Koin** di `di/AppModule.kt`: `single<I{M}Service> { {M}Service() }`
6. **DTOs** `modules/{m}/dto/{M}Dto.kt` — data class (whitelist field eksplisit, anti mass-assignment)
7. **Routes** `modules/{m}/routes/{M}Routes.kt` — `fun Application.{m}Module()`, gunakan `namedGet`/`namedPost`/`namedPut`/`namedDelete` DSL
8. **Register** di `Application.kt` → `configureRouting()`
9. **FreeMarker templates** `resources/templates/{m}/...` — selalu `<#ftl output_format="HTML">` di baris 1
10. **Test** `src/test/kotlin/com/kotlinadmin/{integration|api}/`
11. **Docs** — update `README.md` + `docs/API.md` bila ada API

---

## Security Checklist

- Handler admin: `call.requireAuthenticated()` + `call.checkAccess(routeName, method)` di **setiap** handler.
- Form mutasi web: CSRF token di-inject via `respondView` (field `_csrf`) → form pakai `?_csrf=${_csrf}` di action URL.
- Endpoint sensitif (login/register/OTP): dibungkus `rateLimited(RateLimitName("auth"))`.
- Upload: validasi magic-byte (16 byte pertama `InputStream`), whitelist ekstensi, re-encode gambar.
- JWT: blacklist token via Redis (`setex jti ttl "1"`) saat logout; verifikasi signature + exp + blacklist.
- Secret fail-fast: `AppConfig` validasi JWT_SECRET dan SESSION_SECRET di startup (min 32 chars di produksi).
- Jangan bocorkan detail error ke user: `StatusPages` kirim pesan generik di production.

---

## DO NOT (akan ditolak `./gradlew check`)

- ❌ `System.getenv(...)` di `src/main/kotlin/com/kotlinadmin/modules/` → rule Detekt `NoSystemEnvInModules`
- ❌ Service tanpa `I*Service` interface → anti Dependency Inversion
- ❌ Exposed entity access di luar `dbQuery { }` → `LazyInitializationException`
- ❌ `try/catch(AppException)` per-handler → pakai StatusPages
- ❌ `UUIDTable` (id UUID native) → pakai `IdTable<String>` + `varchar(36)`
- ❌ Hardcode secret/password/credential di source
- ❌ `ManyToMany` tanpa PIN nama tabel join → gunakan `object UsersRoles : Table("users_roles")`
- ❌ Template FreeMarker tanpa `<#ftl output_format="HTML">` → XSS risk
- ❌ Modul tanpa test

---

## Definition of Done (modul/fitur)

- [ ] Mengikuti alur + checklist + prinsip di atas
- [ ] `./gradlew detekt` → 0 issues
- [ ] `./gradlew test` → hijau (termasuk test baru)
- [ ] Security checklist terpenuhi
- [ ] `README.md` + `docs/API.md` diperbarui

---

## Perintah Penting

```bash
./gradlew run                        # jalankan dev (auto-migrate + seed)
./gradlew test                       # semua test
./gradlew detekt                     # convention checker
./gradlew check                      # detekt + test (CI gate)
./gradlew makeModule -Pmodule=Nama   # generate skeleton modul baru
./gradlew addUi                      # upgrade API-only → Full mode
./gradlew wrapper --gradle-version 8.7  # regenerate Gradle wrapper
```

---

## Pola Acuan Termutakhir

Modul `access` (User/Role/Permission) adalah **referensi pola yang utuh**:
- Entity: `src/main/kotlin/com/kotlinadmin/modules/access/models/Tables.kt`
- Service: `modules/access/services/UserService.kt` + `IUserService.kt`
- Routes: `modules/access/routes/UserRoutes.kt`
- Templates: `src/main/resources/templates/access/users/`
- Tests: `src/test/kotlin/com/kotlinadmin/integration/UserServiceTest.kt`
