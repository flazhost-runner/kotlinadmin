package com.kotlinadmin.bdd.steps

import com.kotlinadmin.extractJwtToken
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertTrue

/** Email milik skenario BDD — dibersihkan sebelum tiap login agar skenario idempoten. */
private val BDD_TEST_EMAILS = listOf(
    "test@example.com",
    "delete-me@example.com",
    "bulk1@example.com",
    "bulk2@example.com"
)

private val CSRF_REGEX = Regex("""_csrf=([0-9a-fA-F\-]{36})""")

class AccessStepDefs {

    @Given("I am logged in as admin")
    fun iAmLoggedInAsAdmin() = runBlocking {
        val client = SharedTestState.client
        val resp = client.post("/auth/login") {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("email=admin%40admin.com&password=12345678")
        }
        SharedTestState.sessionCookies =
            resp.headers.getAll(HttpHeaders.SetCookie)?.joinToString("; ") { it.split(";")[0] } ?: ""

        // Ambil token CSRF asli (UUID di session) dari halaman yang dirender.
        val page = client.get("/admin/v1/access/user") {
            header(HttpHeaders.Cookie, SharedTestState.sessionCookies)
        }
        SharedTestState.csrfToken = CSRF_REGEX.find(page.bodyAsText())?.groupValues?.get(1)

        val apiResp = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"admin@admin.com","password":"12345678"}""")
        }
        SharedTestState.jwtToken = extractJwtToken(apiResp.bodyAsText())

        cleanupTestUsers()
    }

    /** Hapus sisa user uji dari run sebelumnya via API agar skenario deterministik. */
    private suspend fun cleanupTestUsers() {
        val jwt = SharedTestState.jwtToken ?: return
        val client = SharedTestState.client
        for (email in BDD_TEST_EMAILS) {
            val listResp = client.get("/api/v1/access/user?q_email=${email.encodeURLParameter()}") {
                header(HttpHeaders.Authorization, "Bearer $jwt")
            }
            val ids = parseUserIds(listResp.bodyAsText())
            for (id in ids) {
                client.delete("/api/v1/access/user/$id/delete") {
                    header(HttpHeaders.Authorization, "Bearer $jwt")
                }
            }
        }
    }

    @Given("a user exists with email {string} and code {string}")
    fun aUserExistsWithEmailAndCode(email: String, code: String) = runBlocking {
        val csrf = requireNotNull(SharedTestState.csrfToken) { "CSRF token not captured — login first" }
        SharedTestState.client.post("/admin/v1/access/user/store?_csrf=$csrf") {
            contentType(ContentType.Application.FormUrlEncoded)
            header(HttpHeaders.Cookie, SharedTestState.sessionCookies)
            setBody(
                "code=${code.encodeURLParameter()}" +
                    "&name=Test+User" +
                    "&email=${email.encodeURLParameter()}" +
                    "&password=password123" +
                    "&passwordConfirm=password123" +
                    "&status=Active" +
                    "&timezone=UTC"
            )
        }
        SharedTestState.createdUserIds.add(email)
    }

    @When("I GET {string} with JWT")
    fun iGetWithJwt(path: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.get(path) {
            val jwt = SharedTestState.jwtToken
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
        }
    }

    @When("I POST to {string} with CSRF and form fields:")
    fun iPostWithCsrfAndFormFields(path: String, table: io.cucumber.datatable.DataTable) = runBlocking {
        val csrf = requireNotNull(SharedTestState.csrfToken) { "CSRF token not captured — login first" }
        val fields = table.asMap<String, String>(String::class.java, String::class.java)
        val body = fields.entries.joinToString("&") { (k, v) ->
            "${k.encodeURLParameter()}=${v.encodeURLParameter()}"
        }
        SharedTestState.lastResponse = SharedTestState.client.post("$path?_csrf=$csrf") {
            contentType(ContentType.Application.FormUrlEncoded)
            header(HttpHeaders.Cookie, SharedTestState.sessionCookies)
            setBody(body)
        }
    }

    @When("I POST to delete the user with method override {string} and CSRF token")
    fun iDeleteUser(methodOverride: String) = runBlocking {
        val csrf = requireNotNull(SharedTestState.csrfToken) { "CSRF token not captured — login first" }
        val email = SharedTestState.createdUserIds.firstOrNull() ?: return@runBlocking
        val apiListResp = SharedTestState.client.get("/api/v1/access/user?q_email=${email.encodeURLParameter()}") {
            val jwt = SharedTestState.jwtToken
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        val userId = parseUserIds(apiListResp.bodyAsText()).firstOrNull()

        if (userId != null) {
            SharedTestState.lastCreatedUserId = userId
            SharedTestState.lastResponse = SharedTestState.client.post(
                "/admin/v1/access/user/$userId/delete?$methodOverride&_csrf=$csrf"
            ) {
                contentType(ContentType.Application.FormUrlEncoded)
                header(HttpHeaders.Cookie, SharedTestState.sessionCookies)
                setBody("")
            }
        }
    }

    @When("I POST to {string} with CSRF and selected user ids")
    fun iPostDeleteSelected(path: String) = runBlocking {
        val csrf = requireNotNull(SharedTestState.csrfToken) { "CSRF token not captured — login first" }
        val apiResp = SharedTestState.client.get("/api/v1/access/user") {
            val jwt = SharedTestState.jwtToken
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
        }
        // Hanya user yang dibuat skenario ini — jangan ikut menghapus admin/seed data.
        val ids = parseUsers(apiResp.bodyAsText())
            .filter { (_, email) -> email in SharedTestState.createdUserIds }
            .map { (id, _) -> id }

        val selectedBody = ids.joinToString("&") { "selected%5B%5D=$it" }
        SharedTestState.lastResponse = SharedTestState.client.post("$path?_csrf=$csrf") {
            contentType(ContentType.Application.FormUrlEncoded)
            header(HttpHeaders.Cookie, SharedTestState.sessionCookies)
            setBody(selectedBody)
        }
    }

    @And("the response contains {string}")
    fun theResponseContains(text: String) = runBlocking {
        val body = requireNotNull(SharedTestState.lastResponse).bodyAsText()
        assertTrue(body.contains(text, ignoreCase = true), "Expected body to contain '$text'")
    }

    /** Index API memakai amplop {status, message, data: {data: [...], pagination}}. */
    private fun parseUserIds(body: String): List<String> = parseUsers(body).map { (id, _) -> id }

    private fun parseUsers(body: String): List<Pair<String, String>> = try {
        Json.parseToJsonElement(body).jsonObject["data"]
            ?.jsonObject?.get("data")
            ?.let { it as? JsonArray }
            ?.mapNotNull { el ->
                val obj = el.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val email = obj["email"]?.jsonPrimitive?.content ?: ""
                id to email
            }
            ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }
}
