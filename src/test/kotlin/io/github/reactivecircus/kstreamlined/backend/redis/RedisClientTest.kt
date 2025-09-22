package io.github.reactivecircus.kstreamlined.backend.redis

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RedisClientTest {
    @Test
    fun `get(key) returns result when API call succeeds`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.rawSegments.last()) {
                "a" -> respond(content = "{ \"result\": null }")
                "b" -> respond(content = "{ \"result\": 3 }")
                "c" -> respond(content = "{ \"result\": \"foo\" }")
                "d" -> respond(
                    content = """
                        { "result": "[\"a\",\"b\",\"c\"]" }
                    """.trimIndent(),
                )

                else -> respond(
                    content = """
                        { "result": "{\"key\": \"value\"}" }
                    """.trimIndent(),
                )
            }
        }
        val redisClient = createRedisClient(mockEngine)

        assert(redisClient.get("a") == null)
        assert(redisClient.get("b") == "3")
        assert(redisClient.get("c") == "foo")
        assert(redisClient.get("d") == "[\"a\",\"b\",\"c\"]")
        assert(redisClient.get("e") == "{\"key\": \"value\"}")
    }

    @Test
    fun `get(key) returns null when API call fails`() = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val redisClient = createRedisClient(mockEngine)

        assert(redisClient.get("a") == null)
    }

    @Test
    fun `get(key) throws any unknown exceptions`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            throw IOException("Unknown exception")
        }
        val redisClient = createRedisClient(mockEngine)

        assertFailsWith<IOException> {
            redisClient.get("a")
        }
    }

    @Test
    fun `set(key, value) calls API with expected key, value, and expiry parameter`() = runBlocking {
        val mockEngine = MockEngine {
            respond(content = "{ \"result\": \"OK\" }")
        }
        val redisClient = createRedisClient(mockEngine)

        redisClient.set("a", "3", keyExpirySeconds = 10)

        assert(mockEngine.requestHistory[0].url.rawSegments.last() == "a")
        assert(mockEngine.requestHistory[0].url.encodedQuery == "EX=10")
        assert(mockEngine.requestHistory[0].body.toString() == "TextContent[application/json] \"3\"")
        assert(mockEngine.responseHistory[0].statusCode == HttpStatusCode.OK)
    }

    @Test
    fun `set(key, value) does not throw when API call fails`() = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.InternalServerError)
        }
        val redisClient = createRedisClient(mockEngine)

        redisClient.set("a", "3")

        assert(mockEngine.requestHistory[0].url.rawSegments.last() == "a")
        assert(mockEngine.requestHistory[0].url.encodedQuery == "EX=3600")
        assert(mockEngine.requestHistory[0].body.toString() == "TextContent[application/json] \"3\"")
        assert(mockEngine.responseHistory[0].statusCode == HttpStatusCode.InternalServerError)
    }

    @Test
    fun `set(key, value) throws any unknown exceptions`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            throw IOException("Unknown exception")
        }
        val redisClient = createRedisClient(mockEngine)

        assertFailsWith<IOException> {
            redisClient.set("a", "3")
        }
    }

    private fun createRedisClient(mockEngine: MockEngine) = RedisClient(
        engine = mockEngine,
        url = "",
        token = "",
    )
}
