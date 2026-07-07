# Testing — KotlinAdmin

Suite pengujian menyeluruh: **Unit, Integration, API, Security, BDD**. CI menjalankan Detekt + test + matrix DB.

## Stack

| Layer | Tool |
|-------|------|
| Test runner | Kotest 5.9.1 (JUnit5 engine) |
| BDD | Cucumber-JVM 7.20.1 (cucumber-kotlin) |
| HTTP test | `ktor-server-test-host` 3.1.3 (`testApplication`) |
| DB test | `org.xerial:sqlite-jdbc` — SQLite in-memory |
| Redis | di-mock (fake in-memory implementation) |

## Konfigurasi

### `src/test/resources/application.conf`

```hocon
ktor.deployment.port = 0

app {
  name = "KotlinAdmin-Test"
  mode = "full"
  jwtSecret = "test-jwt-secret-at-least-32-chars-long!"
  jwtExpireHours = 1
  sessionSecret = "test-session-secret-at-least-32-chars!"
  bcryptRounds = 4
}

database {
  url      = "jdbc:sqlite::memory:"
  driver   = "org.sqlite.JDBC"
  user     = ""
  password = ""
}

redis {
  host = "localhost"
  port = 6379
}
```

### Test setup

```kotlin
// src/test/kotlin/com/kotlinadmin/setup/TestSetup.kt
object TestSetup {
    fun setupDatabase() {
        DatabaseConfig.setup(DbConfig(
            url = "jdbc:sqlite::memory:",
            driver = "org.sqlite.JDBC",
            user = "", password = ""
        ))
        transaction {
            SchemaUtils.create(Users, Roles, Permissions, UsersRoles, RolesPermissions, Settings)
        }
    }

    fun seedAdmin(): UserEntity = transaction {
        val adminRole = RoleEntity.new(UUID.randomUUID().toString()) {
            name = "Administrator"; status = "Active"
            createdAt = System.currentTimeMillis(); updatedAt = System.currentTimeMillis()
        }
        val user = UserEntity.new(UUID.randomUUID().toString()) {
            code = "ADM001"; name = "Admin"; email = "admin@admin.com"
            password = BCrypt.hashpw("12345678", BCrypt.gensalt(4))
            status = "Active"; timezone = "UTC"
            createdAt = System.currentTimeMillis(); updatedAt = System.currentTimeMillis()
        }
        UsersRoles.insert { it[userId] = user.id.value; it[roleId] = adminRole.id.value }
        user
    }
}
```

## Struktur

```
src/test/kotlin/com/kotlinadmin/
├── setup/
│   ├── TestSetup.kt          # DB init, seed helpers
│   └── FakeRedis.kt          # in-memory Redis mock
├── unit/                     # helper murni (no DB)
│   └── HelperTest.kt
├── integration/              # service ↔ SQLite in-memory
│   ├── UserServiceTest.kt
│   ├── RoleServiceTest.kt
│   └── PermissionServiceTest.kt
├── api/                      # HTTP via testApplication
│   ├── AuthApiTest.kt
│   └── UserApiTest.kt
├── security/                 # RBAC, CSRF, JWT blacklist, rate-limit
│   └── SecurityTest.kt
└── bdd/
    └── steps/
        ├── AuthStepDefs.kt
        └── AccessUserStepDefs.kt

src/test/resources/
├── application.conf
├── features/
│   ├── auth.feature
│   └── access_user.feature
└── cucumber.properties
```

## Menjalankan

```bash
./gradlew test                               # semua test
./gradlew test --tests "*.integration.*"     # hanya integration
./gradlew test --tests "*.api.*"             # hanya API
./gradlew test --tests "*.bdd.*"             # hanya BDD
./gradlew test --tests "*.security.*"        # hanya security
./gradlew test --info                        # output verbose
```

## Tipe Test + Contoh

### 1. Unit Test (Kotest DescribeSpec)

```kotlin
// unit/HelperTest.kt
class PaginationHelperTest : DescribeSpec({
    describe("PaginationHelper.build") {
        it("computes totalPages correctly") {
            val result = PaginationHelper.build(emptyList<String>(), total = 25, page = 1, pageSize = 10)
            result.paginateData.totalPages shouldBe 3
        }
        it("computes offset correctly") {
            val result = PaginationHelper.build(emptyList<String>(), total = 30, page = 3, pageSize = 10)
            result.paginateData.offset shouldBe 20
        }
    }
})
```

### 2. Integration Test (service ↔ SQLite)

```kotlin
// integration/UserServiceTest.kt
class UserServiceTest : DescribeSpec({
    beforeSpec {
        TestSetup.setupDatabase()
    }
    beforeEach {
        transaction { Users.deleteAll(); Roles.deleteAll(); UsersRoles.deleteAll() }
    }

    val service = UserService()

    describe("UserService") {
        it("stores user and rejects duplicate email") {
            val dto = UserCreateDto(
                code = "U001", name = "Alice",
                email = "alice@example.com", password = "secret1234",
                status = "Active", timezone = "UTC"
            )
            val user = service.store(dto, "actor-id")
            user.email shouldBe "alice@example.com"
            shouldThrow<ConflictError> { service.store(dto, "actor-id") }
        }
    }
})
```

### 3. API Test (testApplication)

```kotlin
// api/AuthApiTest.kt
class AuthApiTest : DescribeSpec({
    beforeSpec { TestSetup.setupDatabase(); TestSetup.seedAdmin() }

    describe("POST /api/v1/auth/login") {
        it("returns JWT on valid credentials") {
            testApplication {
                application { module() }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                response.status shouldBe HttpStatusCode.OK
                val body = response.bodyAsText()
                body shouldContain "token"
            }
        }
        it("returns 401 on wrong password") {
            testApplication {
                application { module() }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"wrongpwd"}""")
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }
})
```

### 4. Security Test

```kotlin
// security/SecurityTest.kt
class SecurityTest : DescribeSpec({
    beforeSpec { TestSetup.setupDatabase(); TestSetup.seedAdmin() }

    describe("RBAC") {
        it("redirects unauthenticated to /auth/login") {
            testApplication {
                application { module() }
                val response = client.get("/admin/v1/access/user") {
                    // no session cookie
                }
                response.status shouldBe HttpStatusCode.Found
                response.headers["Location"] shouldContain "/auth/login"
            }
        }
    }

    describe("JWT blacklist") {
        it("rejects blacklisted token after logout") {
            testApplication {
                application { module() }
                // 1. login
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = /* parse token from loginResp */ ""
                // 2. access /api/v1/auth/me → 200
                // 3. logout
                // 4. access /api/v1/auth/me with same token → 401
            }
        }
    }
})
```

### 5. BDD (Cucumber)

```gherkin
# features/auth.feature
Feature: Authentication

  Scenario: Admin login with valid credentials
    Given I have valid admin credentials
    When I POST to "/api/v1/auth/login" with those credentials
    Then the response status should be 200
    And the response should contain a JWT token

  Scenario: Login with wrong password
    When I POST to "/api/v1/auth/login" with wrong password
    Then the response status should be 401
```

```kotlin
// bdd/steps/AuthStepDefs.kt
class AuthStepDefs : En {
    private lateinit var response: HttpResponse

    init {
        Given("I have valid admin credentials") { /* setup */ }
        When("I POST to {string} with those credentials") { path: String ->
            // use testApplication or HttpClient
        }
        Then("the response status should be {int}") { code: Int ->
            response.status.value shouldBe code
        }
    }
}
```

```properties
# cucumber.properties
cucumber.publish.quiet=true
cucumber.features=classpath:features
cucumber.glue=com.kotlinadmin.bdd.steps
```

## CI

`.github/workflows/ci.yml`:
- **check** — `./gradlew detekt` + `./gradlew test`
- **db-compat** — migration test di matrix SQLite + MySQL + PostgreSQL

> E2E (browser) dijalankan lokal saja, bukan di CI.

## Postman (manual API testing)

Collection: [`docs/postman/KotlinAdmin.postman_collection.json`](postman/KotlinAdmin.postman_collection.json).

1. Import collection ke Postman.
2. Set variable `base_url` = `http://localhost:8002` (default `APP_PORT` di `application.conf`).
3. Jalankan `POST /api/v1/auth/login` — token otomatis tersimpan ke variable `access_token` untuk request lain.
