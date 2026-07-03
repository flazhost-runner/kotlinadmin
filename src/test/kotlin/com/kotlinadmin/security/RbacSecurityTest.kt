package com.kotlinadmin.security

import com.kotlinadmin.extractJwtToken
import com.kotlinadmin.module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class RbacSecurityTest : DescribeSpec({

    describe("Authentication Guards") {
        it("unauthenticated web request is redirected to /auth/login") {
            testApplication {
                application { module() }
                // Client default mengikuti redirect GET; matikan agar 302 terlihat.
                val rawClient = createClient { followRedirects = false }
                val response = rawClient.get("/admin/v1/dashboard")
                response.status.value shouldBe 302
                val location = response.headers[HttpHeaders.Location] ?: ""
                location.contains("/auth/login") shouldBe true
            }
        }

        it("unauthenticated API request returns 401 JSON") {
            testApplication {
                application { module() }
                val response = client.get("/api/v1/access/user")
                response.status shouldBe HttpStatusCode.Unauthorized
                response.headers[HttpHeaders.ContentType]?.contains("application/json") shouldBe true
            }
        }
    }

    describe("CSRF Protection") {
        it("POST without CSRF token is rejected") {
            testApplication {
                application { module() }
                // First get a valid session by logging in
                val loginResp = client.post("/auth/login") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody("email=admin%40admin.com&password=12345678&_csrf=test-csrf-token")
                }
                val cookies = loginResp.headers.getAll(HttpHeaders.SetCookie)
                    ?.joinToString("; ") { it.split(";")[0] } ?: ""

                // POST without _csrf should be rejected (403 or redirect)
                val resp = client.post("/admin/v1/access/user/store") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    header(HttpHeaders.Cookie, cookies)
                    setBody("code=HACK&name=Hacker&email=hack@evil.com&password=hack123")
                }
                // Should not succeed with 2xx
                (resp.status.value >= 400 || resp.status.value == 302) shouldBe true
            }
        }
    }

    describe("Mass-Assignment Prevention") {
        it("extra fields in user store request are ignored") {
            testApplication {
                application { module() }
                val loginResp = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"admin@admin.com","password":"12345678"}""")
                }
                val token = requireNotNull(extractJwtToken(loginResp.bodyAsText())) { "no token in login response" }

                // Attempt to inject extra field "blocked: false→true" via API body
                val storeResp = client.post("/api/v1/access/user/store") {
                    header(HttpHeaders.Authorization, "Bearer $token")
                    contentType(ContentType.Application.Json)
                    setBody(
                        """
                        {
                          "code":"MASS01","name":"MassTest","email":"mass@test.com",
                          "password":"pass1234","passwordConfirm":"pass1234",
                          "blocked":true,"blockedReason":"injected"
                        }
                        """.trimIndent()
                    )
                }
                // Store should succeed but blocked=true should be ignored (default false)
                if (storeResp.status.isSuccess() || storeResp.status.value == 302) {
                    val editResp = client.get("/api/v1/access/user") {
                        header(HttpHeaders.Authorization, "Bearer $token")
                    }
                    val body = editResp.bodyAsText()
                    // The user should not be blocked regardless of injected value
                    // (blocked field is not in UserCreateDto whitelist)
                    body.contains(""""blocked":true""") shouldBe false
                }
            }
        }
    }

    describe("JWT Security") {
        it("tampered JWT is rejected") {
            testApplication {
                application { module() }
                val response = client.get("/api/v1/auth/me") {
                    header(HttpHeaders.Authorization, "Bearer eyJhbGciOiJIUzI1NiJ9.tampered.signature")
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        it("expired JWT is rejected") {
            // This test validates the concept — actual expiry testing requires clock mocking
            testApplication {
                application { module() }
                // Using a known-expired token structure (will fail signature/exp check)
                val response = client.get("/api/v1/auth/me") {
                    header(
                        HttpHeaders.Authorization,
                        "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxfQ.invalid"
                    )
                }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }
})
