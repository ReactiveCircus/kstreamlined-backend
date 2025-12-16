package io.github.reactivecircus.kstreamlined.backend

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.security.MessageDigest
import kotlin.test.BeforeTest
import kotlin.test.Test

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [KSBackendApplication::class],
)
@Import(TestKSConfiguration::class)
class ApolloPersistedQueriesTest {
    @LocalServerPort
    private var port: Int = 0

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
            .expectBody(String::class.java)
            .consumeWith { response ->
                val body = response.responseBody
                assert(body?.contains("PersistedQueryNotFound") == true) {
                    "Expected PersistedQueryNotFound error, got: $body"
                }
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
                assert(body?.contains("feedSources") == true) {
                    "Registration should return feedSources: $body"
                }
                assert(body?.contains("errors") != true) {
                    "Registration should not have errors: $body"
                }
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
            .expectBody(String::class.java)
            .consumeWith { response ->
                val body = response.responseBody
                assert(body?.contains("feedSources") == true) {
                    "Retrieval by hash should return feedSources: $body"
                }
                assert(body?.contains("errors") != true) {
                    "Retrieval should not have errors: $body"
                }
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
