package io.github.reactivecircus.kstreamlined.backend.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

class DataLoaderTest {
    private val localCache = Caffeine.newBuilder().build<String, List<Int>>()

    @Test
    fun `returns data from local cache if present`() = runBlocking {
        localCache.put("key", listOf(1, 2, 3))

        val dataLoader = createDataLoader(localCache = localCache)

        val result = dataLoader.load("key") {
            suspendCancellableCoroutine {}
        }

        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `returns data from remote cache if local cache is absent and remote cache is present`() = runBlocking {
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = MockEngine { respond(content = "{ \"result\": \"[1, 2, 3]\" }") },
        )

        val result = dataLoader.load("key") {
            suspendCancellableCoroutine {}
        }

        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `returns data from sot when both local and remote caches are absent and sot request succeeds`() = runBlocking {
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = MockEngine { respond(content = "{ \"result\": null }") },
        )

        val result = dataLoader.load("key") {
            listOf(1, 2, 3)
        }

        assert(result == listOf(1, 2, 3))
    }

    @Test
    fun `returns data from sot and overwrites remote cache when remote cache value cannot be decoded`() = runBlocking {
        val redisMockEngine = redisMockEngine(getResult = "\"[{\\\"legacy\\\": 1}]\"")
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = redisMockEngine,
        )

        val result = dataLoader.load("key") {
            listOf(1, 2, 3)
        }

        assertEquals(listOf(1, 2, 3), result)
        assertEquals(listOf(1, 2, 3), localCache.getIfPresent("key"))
        assertRemoteCacheWritten(redisMockEngine, key = "key", value = "[1,2,3]", expiry = 1.days)
    }

    @Test
    fun `propagates exception when remote cache throws`(): Unit = runBlocking {
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = MockEngine { throw IOException("Unknown exception") },
        )

        assertFailsWith<IOException> {
            dataLoader.load("key") {
                suspendCancellableCoroutine {}
            }
        }
    }

    @Test
    fun `throws exception when sot request fails`(): Unit = runBlocking {
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = MockEngine { respond(content = "{ \"result\": null }") },
        )

        assertFailsWith<IOException> {
            dataLoader.load("key") {
                throw IOException("Server error")
            }
        }
    }

    @Test
    fun `updates local cache after loading data from remote cache`() = runBlocking {
        val dataLoader = createDataLoader(
            localCache = localCache,
            redisMockEngine = MockEngine { respond(content = "{ \"result\": \"[1, 2, 3]\" }") },
        )

        dataLoader.load("key") {
            suspendCancellableCoroutine {}
        }

        assertEquals(listOf(1, 2, 3), localCache.getIfPresent("key"))
    }

    @Test
    fun `updates both local and remote caches after loading data from sot`() = runBlocking {
        val redisMockEngine = redisMockEngine()
        val dataLoader = createDataLoader(
            localCache = localCache,
            remoteCacheExpiry = 1.hours,
            redisMockEngine = redisMockEngine,
        )

        dataLoader.load("key") {
            listOf(1, 2, 3)
        }

        assertEquals(listOf(1, 2, 3), localCache.getIfPresent("key"))
        assertRemoteCacheWritten(redisMockEngine, key = "key", value = "[1,2,3]", expiry = 1.hours)
    }

    @Test
    fun `only refreshes remote cache expiry when value from sot is unchanged`() = runBlocking {
        val redisMockEngine = redisMockEngine(remoteChecksumMatches = true)
        val dataLoader = createDataLoader(
            localCache = localCache,
            remoteCacheExpiry = 1.hours,
            redisMockEngine = redisMockEngine,
        )

        dataLoader.load("key", sotOnly = true) {
            listOf(1, 2, 3)
        }

        assertEquals(listOf(1, 2, 3), localCache.getIfPresent("key"))
        assertEquals(1, redisMockEngine.requestHistory.size)
        assertEquals("EVAL", redisMockEngine.requestHistory.single().jsonBody().jsonArray[0].jsonPrimitive.content)
    }

    @Test
    fun `skips both local and remote caches when loading with sotOnly = true`() = runBlocking {
        localCache.put("key", listOf(1, 2))

        val redisMockEngine = redisMockEngine(getResult = "\"[1, 2, 3]\"")
        val dataLoader = createDataLoader(
            localCache = localCache,
            remoteCacheExpiry = 1.hours,
            redisMockEngine = redisMockEngine,
        )

        dataLoader.load("key", sotOnly = true) {
            listOf(1, 2, 3, 4)
        }

        assertEquals(listOf(1, 2, 3, 4), localCache.getIfPresent("key"))
        assertTrue(redisMockEngine.requestHistory.none { it.url.encodedPath.contains("/get/") })
        assertRemoteCacheWritten(redisMockEngine, key = "key", value = "[1,2,3,4]", expiry = 1.hours)
    }

    private fun redisMockEngine(getResult: String = "null", remoteChecksumMatches: Boolean = false) = MockEngine {
        when {
            it.url.encodedPath.contains("/get/") -> respond(content = "{ \"result\": $getResult }")
            it.url.encodedPath.endsWith("/multi-exec") -> respond(content = "[{ \"result\": \"OK\" }, { \"result\": \"OK\" }]")
            else -> respond(content = "{ \"result\": ${if (remoteChecksumMatches) 1 else 0} }")
        }
    }

    private fun assertRemoteCacheWritten(redisMockEngine: MockEngine, key: String, value: String, expiry: Duration) {
        val request = redisMockEngine.requestHistory.last()
        assertTrue(request.url.encodedPath.endsWith("/multi-exec"))
        val setValue = request.jsonBody().jsonArray[0].jsonArray.map { it.jsonPrimitive.content }
        assertEquals(listOf("SET", key, value, "EX", expiry.inWholeSeconds.toString()), setValue)
        assertEquals(HttpStatusCode.OK, redisMockEngine.responseHistory.last().statusCode)
    }

    private fun HttpRequestData.jsonBody() = Json.parseToJsonElement((body as TextContent).text)

    private fun createDataLoader(
        localCache: Cache<String, List<Int>>,
        remoteCacheExpiry: Duration = 1.days,
        redisMockEngine: MockEngine = MockEngine { respond(content = "{ \"result\": null }") },
    ) = DataLoader.of(
        localCache = localCache,
        remoteCacheExpiry = remoteCacheExpiry,
        redisClient = RedisClient(
            engine = redisMockEngine,
            url = "",
            token = "",
        ),
        serializer = Int.serializer(),
    )
}
