package com.kotlinadmin.bdd.steps

import com.kotlinadmin.extractJwtToken
import io.cucumber.java.en.And
import io.cucumber.java.en.Given
import io.cucumber.java.en.When
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertTrue

class AuthStepDefs {

    @Given("the application is running")
    fun theApplicationIsRunning() {
        requireNotNull(SharedTestState.testApp) { "TestApplication was not created in @Before" }
    }

    @When("I POST to {string} with email {string} and password {string}")
    fun iPostFormLogin(path: String, email: String, password: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.post(path) {
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(
                "email=${email.encodeURLParameter()}" +
                    "&password=${password.encodeURLParameter()}"
            )
        }
        SharedTestState.sessionCookies = SharedTestState.lastResponse!!.headers[HttpHeaders.SetCookie] ?: ""
    }

    @When("I POST to {string} with JSON body email {string} password {string}")
    fun iPostJsonLogin(path: String, email: String, password: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.post(path) {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        if (SharedTestState.lastResponse!!.status.isSuccess()) {
            SharedTestState.jwtToken = extractJwtToken(SharedTestState.lastResponse!!.bodyAsText())
        }
    }

    @Given("I am logged in via API as {string} with password {string}")
    fun iAmLoggedInViaApi(email: String, password: String) = runBlocking {
        val resp = SharedTestState.client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email":"$email","password":"$password"}""")
        }
        SharedTestState.jwtToken = extractJwtToken(resp.bodyAsText())
    }

    @When("I GET {string} without authentication")
    fun iGetWithoutAuth(path: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.get(path)
    }

    @When("I GET {string} with the same token")
    fun iGetWithSameToken(path: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.get(path) {
            val jwt = SharedTestState.jwtToken
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
        }
    }

    @When("I POST to {string} with JWT")
    fun iPostWithJwt(path: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.post(path) {
            val jwt = SharedTestState.jwtToken
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            contentType(ContentType.Application.Json)
            setBody("{}")
        }
    }

    @When("I POST to {string}")
    fun iPost(path: String) = runBlocking {
        SharedTestState.lastResponse = SharedTestState.client.post(path) {
            val jwt = SharedTestState.jwtToken
            val cookies = SharedTestState.sessionCookies
            if (jwt != null) header(HttpHeaders.Authorization, "Bearer $jwt")
            if (cookies.isNotBlank()) header(HttpHeaders.Cookie, cookies)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody("")
        }
    }

    @And("the response JSON has field {string}")
    fun theResponseJsonHasField(field: String) = runBlocking {
        val body = requireNotNull(SharedTestState.lastResponse).bodyAsText()
        val json = Json.parseToJsonElement(body).jsonObject
        // Respons API memakai amplop {status, message, data} — field dicek di level
        // atas maupun di dalam data.
        val found = json.containsKey(field) ||
            json["data"]?.jsonObject?.containsKey(field) == true
        assertTrue(found, "Expected JSON to contain field '$field', got: $body")
    }
}
