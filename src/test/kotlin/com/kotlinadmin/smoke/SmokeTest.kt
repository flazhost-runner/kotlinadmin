package com.kotlinadmin.smoke

import com.kotlinadmin.module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*

class SmokeTest : DescribeSpec({

    describe("Smoke Tests") {

        it("app starts and home page (/) returns 200") {
            testApplication {
                application { module() }
                val response = client.get("/")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("/auth/login is accessible and returns 200") {
            testApplication {
                application { module() }
                val response = client.get("/auth/login")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("/auth/register returns 200") {
            testApplication {
                application { module() }
                val response = client.get("/auth/register")
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("web routes return Content-Type: text/html") {
            testApplication {
                application { module() }
                val response = client.get("/auth/login")
                val ct = response.headers[HttpHeaders.ContentType] ?: ""
                ct.contains("text/html") shouldBe true
            }
        }

        it("API auth route returns Content-Type: application/json") {
            testApplication {
                application { module() }
                val response = client.post("/api/v1/auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody("""{"email":"nobody@example.com","password":"wrong"}""")
                }
                val ct = response.headers[HttpHeaders.ContentType] ?: ""
                ct.contains("application/json") shouldBe true
            }
        }

        it("login page contains 'Hello, Welcome Back!'") {
            testApplication {
                application { module() }
                val response = client.get("/auth/login")
                response.bodyAsText() shouldContain "Hello, Welcome Back!"
            }
        }

        it("unauthenticated /admin/v1/dashboard redirects to /auth/login") {
            testApplication {
                application { module() }
                // Client default mengikuti redirect GET; matikan agar 302 terlihat.
                val rawClient = createClient { followRedirects = false }
                val response = rawClient.get("/admin/v1/dashboard")
                response.status.value shouldBe 302
                val location = response.headers[HttpHeaders.Location] ?: ""
                location shouldContain "/auth/login"
            }
        }

        it("health: DB connection and seed data exist (admin login succeeds)") {
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
    }
})
