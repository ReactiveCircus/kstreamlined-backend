package io.github.reactivecircus.kstreamlined.backend

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.security.MessageDigest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [KSBackendApplication::class],
)
@Import(TestKSConfiguration::class)
class ApolloPersistedQueriesTest {
    @LocalServerPort
    private val port: Int = 0

    private lateinit var webTestClient: WebTestClient

    private val feedSourcesQuery = """
        query FeedSources {
            feedSources {
                key
                title
            }
        }
    """.trimIndent()

    @BeforeTest
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun `query with persisted query extension returns PersistedQueryNotFound for unknown hash`() {
        val unknownHash = "0".repeat(64)
        val requestBody = """
            {
                "extensions": {
                    "persistedQuery": {
                        "version": 1,
                        "sha256Hash": "$unknownHash"
                    }
                }
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/graphql")
            .header("Content-Type", "application/json")
            .bodyValue(requestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith { response ->
                val body = response.responseBody
                assertEquals(body?.contains("PersistedQueryNotFound"), true)
            }
    }

    @Test
    fun `query can be registered and then retrieved by hash`() {
        val queryHash = feedSourcesQuery.sha256Hash()

        // First request: register the query with the hash
        val registerRequestBody = """
            {
                "query": ${feedSourcesQuery.toJsonString()},
                "extensions": {
                    "persistedQuery": {
                        "version": 1,
                        "sha256Hash": "$queryHash"
                    }
                }
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/graphql")
            .header("Content-Type", "application/json")
            .bodyValue(registerRequestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith { response ->
                val body = response.responseBody
                assertEquals(body?.contains("feedSources"), true)
                assertNotEquals(body?.contains("errors"), true)
            }

        // Second request: retrieve by hash only (no query body)
        val retrieveRequestBody = """
            {
                "extensions": {
                    "persistedQuery": {
                        "version": 1,
                        "sha256Hash": "$queryHash"
                    }
                }
            }
        """.trimIndent()

        webTestClient.post()
            .uri("/graphql")
            .header("Content-Type", "application/json")
            .bodyValue(retrieveRequestBody)
            .exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith { response ->
                val body = response.responseBody
                assertEquals(body?.contains("feedSources"), true)
                assertNotEquals(body?.contains("errors"), true)
            }
    }

    private fun String.sha256Hash(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun String.toJsonString(): String {
        return "\"" + this.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t") + "\""
    }
}
