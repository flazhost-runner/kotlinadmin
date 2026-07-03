package com.kotlinadmin.api

import com.kotlinadmin.extractJwtToken
import com.kotlinadmin.module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class AuthApiTest : DescribeSpec({

    describe("POST /api/v1/auth/login") {
        it("returns 200 with JWT token for valid credentials") {
            testApplication {
                application { module() }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                response.status shouldBe HttpStatusCode.OK
                response.bodyAsText() shouldContain "token"
            }
        }

        it("returns 401 for wrong credentials") {
            testApplication {
                application { module() }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"wrongpassword"}""")
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    describe("GET /api/v1/auth/me") {
        it("returns 401 without auth header") {
            testApplication {
                application { module() }
                val response = client.get("/api/v1/auth/me")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        it("returns 200 with valid JWT") {
            testApplication {
                application { module() }
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = requireNotNull(extractJwtToken(loginResp.bodyAsText())) { "no token in login response" }

                val meResp = client.get("/api/v1/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                meResp.status shouldBe HttpStatusCode.OK
                meResp.bodyAsText() shouldContain "email"
            }
        }
    }

    describe("POST /api/v1/auth/logout") {
        it("blacklists token so subsequent /me returns 401") {
            testApplication {
                application { module() }
                // 1. Login
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = requireNotNull(extractJwtToken(loginResp.bodyAsText())) { "no token in login response" }

                // 2. /me returns 200
                val beforeLogout = client.get("/api/v1/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                beforeLogout.status shouldBe HttpStatusCode.OK

                // 3. Logout
                val logoutResp = client.post("/api/v1/auth/logout") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                logoutResp.status shouldBe HttpStatusCode.OK

                // 4. /me now returns 401 (token blacklisted)
                val afterLogout = client.get("/api/v1/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                afterLogout.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    describe("Verbose API paths (NodeAdmin-style)") {
        it("GET /api/v1/access/user returns 200") {
            testApplication {
                application { module() }
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = requireNotNull(extractJwtToken(loginResp.bodyAsText())) { "no token in login response" }

                val response = client.get("/api/v1/access/user") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("GET /api/v1/access/users (REST-style) returns 404") {
            testApplication {
                application { module() }
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = requireNotNull(extractJwtToken(loginResp.bodyAsText())) { "no token in login response" }

                val response = client.get("/api/v1/access/users") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                response.status shouldBe HttpStatusCode.NotFound
            }
        }
    }
})
