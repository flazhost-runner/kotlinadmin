package com.kotlinadmin.bdd.steps

import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import io.ktor.server.testing.TestApplication

object SharedTestState {
    var testApp: TestApplication? = null

    /**
     * Client tanpa follow-redirect agar skenario dapat memeriksa status 302 +
     * header Location secara langsung (client default Ktor mengikuti redirect GET).
     */
    var httpClient: HttpClient? = null

    val client: HttpClient get() = requireNotNull(httpClient) { "HttpClient was not created in @Before" }

    var lastResponse: HttpResponse? = null
    var jwtToken: String? = null
    var sessionCookies: String = ""

    /** Token CSRF asli yang diekstrak dari halaman setelah login web. */
    var csrfToken: String? = null
    var lastCreatedUserId: String? = null
    val createdUserIds: MutableList<String> = mutableListOf()

    fun reset() {
        httpClient?.close()
        httpClient = null
        testApp?.stop()
        testApp = null
        lastResponse = null
        jwtToken = null
        sessionCookies = ""
        csrfToken = null
        lastCreatedUserId = null
        createdUserIds.clear()
    }
}
