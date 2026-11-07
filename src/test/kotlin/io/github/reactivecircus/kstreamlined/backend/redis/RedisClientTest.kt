package io.github.reactivecircus.kstreamlined.backend.redis

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.seconds

class RedisClientTest {
    @Test
    fun `get(key) returns result when API call succeeds`() = runTest {
        val mockEngine = MockEngine { request ->
            when (request.url.rawSegments.last()) {
                "a" -> respond(content = "{ \"result\": null }")

                "b" -> respond(content = "{ \"result\": 3 }")

                "c" -> respond(content = "{ \"result\": \"foo\" }")

                "d" -> respond(
                    content = """
                        |{ "result": "[\"a\",\"b\",\"c\"]" }
                    """.trimMargin(),
                )

                else -> respond(
                    content = """
                        |{ "result": "{\"key\": \"value\"}" }
                    """.trimMargin(),
                )
            }
        }
        val redisClient = createRedisClient(mockEngine)

        assertNull(redisClient.get("a"))
        assertEquals("3", redisClient.get("b"))
        assertEquals("foo", redisClient.get("c"))
        assertEquals("[\"a\",\"b\",\"c\"]", redisClient.get("d"))
        assertEquals("{\"key\": \"value\"}", redisClient.get("e"))
    }

    @Test
    fun `get(key) returns null when API call fails`() = runTest {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val redisClient = createRedisClient(mockEngine)

        assert(redisClient.get("a") == null)
    }

    @Test
    fun `get(key) throws any unknown exceptions`(): Unit = runTest {
        val mockEngine = MockEngine {
            throw IOException("Unknown exception")
        }
        val redisClient = createRedisClient(mockEngine)

        assertFailsWith<IOException> {
            redisClient.get("a")
        }
    }

    @Test
    fun `setIfChanged(key, value) only refreshes expiry when stored checksum matches`() = runTest {
        val mockEngine = MockEngine {
            respond(content = "{ \"result\": 1 }")
        }
        val redisClient = createRedisClient(mockEngine)

        redisClient.setIfChanged("a", "3", expiry = 10.seconds)

        assertEquals(1, mockEngine.requestHistory.size)
        val command = mockEngine.requestHistory[0].jsonBody().jsonArray.map { it.jsonPrimitive.content }
        assertEquals("EVAL", command[0])
        assertEquals(listOf("2", "a", "a:checksum", Sha256Of3, "10"), command.drop(2))
    }

    @Test
    fun `setIfChanged(key, value) writes value and checksum atomically when stored checksum does not match`() =
        runTest {
            val mockEngine = MockEngine {
                if (it.url.encodedPath.endsWith("/multi-exec")) {
                    respond(content = "[{ \"result\": \"OK\" }, { \"result\": \"OK\" }]")
                } else {
                    respond(content = "{ \"result\": 0 }")
                }
            }
            val redisClient = createRedisClient(mockEngine)

            redisClient.setIfChanged("a", "3", expiry = 10.seconds)

            assertEquals(2, mockEngine.requestHistory.size)
            assertEquals("/multi-exec", mockEngine.requestHistory[1].url.encodedPath)
            val commands = mockEngine.requestHistory[1].jsonBody().jsonArray.map { command ->
                command.jsonArray.map { it.jsonPrimitive.content }
            }
            assertEquals(
                listOf(
                    listOf("SET", "a", "3", "EX", "10"),
                    listOf("SET", "a:checksum", Sha256Of3, "EX", "10"),
                ),
                commands,
            )
        }

    @Test
    fun `setIfChanged(key, value) writes value when checksum check fails`() = runTest {
        val mockEngine = MockEngine {
            if (it.url.encodedPath.endsWith("/multi-exec")) {
                respond(content = "[{ \"result\": \"OK\" }, { \"result\": \"OK\" }]")
            } else {
                respondError(HttpStatusCode.InternalServerError)
            }
        }
        val redisClient = createRedisClient(mockEngine)

        redisClient.setIfChanged("a", "3", expiry = 10.seconds)

        assertEquals(2, mockEngine.requestHistory.size)
        assertEquals("/multi-exec", mockEngine.requestHistory[1].url.encodedPath)
        assertEquals(HttpStatusCode.OK, mockEngine.responseHistory[1].statusCode)
    }

    @Test
    fun `setIfChanged(key, value) does not throw when write fails`() = runTest {
        val responses = listOf(
            "{ \"error\": \"EXECABORT Transaction discarded\" }",
            "[{ \"error\": \"ERR\" }, { \"result\": \"OK\" }]",
            "not json",
        )
        responses.forEach { response ->
            val mockEngine = MockEngine {
                if (it.url.encodedPath.endsWith("/multi-exec")) {
                    respond(content = response)
                } else {
                    respond(content = "{ \"result\": 0 }")
                }
            }
            createRedisClient(mockEngine).setIfChanged("a", "3", expiry = 10.seconds)
            assertEquals(2, mockEngine.requestHistory.size)
        }

        val failingEngine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        createRedisClient(failingEngine).setIfChanged("a", "3", expiry = 10.seconds)
        assertEquals(2, failingEngine.requestHistory.size)
    }

    @Test
    fun `setIfChanged(key, value) throws any unknown exceptions`(): Unit = runTest {
        val mockEngine = MockEngine {
            throw IOException("Unknown exception")
        }
        val redisClient = createRedisClient(mockEngine)

        assertFailsWith<IOException> {
            redisClient.setIfChanged("a", "3", expiry = 10.seconds)
        }
    }

    private fun HttpRequestData.jsonBody() = Json.parseToJsonElement((body as TextContent).text)

    private fun createRedisClient(mockEngine: MockEngine) = RedisClient(
        engine = mockEngine,
        url = "",
        token = "",
    )

    companion object {
        private const val Sha256Of3 = "4e07408562bedb8b60ce05c1decfe3ad16b72230967de01f640b7e4729b49fce"
    }
}
