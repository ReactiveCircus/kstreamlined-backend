package io.github.reactivecircus.kstreamlined.backend.datasource

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.builtins.serializer
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        val redisMockEngine = MockEngine {
            if (it.url.encodedPath.contains("get")) {
                respond(content = "{ \"result\": null }")
            } else {
                respond(content = "{ \"result\": \"OK\" }")
            }
        }
        val dataLoader = createDataLoader(
            localCache = localCache,
            remoteCacheExpiry = 1.hours,
            redisMockEngine = redisMockEngine,
        )

        dataLoader.load("key") {
            listOf(1, 2, 3)
        }

        assertEquals(listOf(1, 2, 3), localCache.getIfPresent("key"))
        assertEquals("key", redisMockEngine.requestHistory.last().url.segments.last())
        assertEquals("EX=${1.hours.inWholeSeconds}", redisMockEngine.requestHistory.last().url.encodedQuery)
        assertEquals("TextContent[application/json] \"[1,2,3]\"", redisMockEngine.requestHistory.last().body.toString())
        assertEquals(HttpStatusCode.OK, redisMockEngine.responseHistory.last().statusCode)
    }

    @Test
    fun `skips both local and remote caches when loading with sotOnly = true`() = runBlocking {
        localCache.put("key", listOf(1, 2))

        val redisMockEngine = MockEngine {
            if (it.url.encodedPath.contains("get")) {
                respond(content = "{ \"result\": \"[1, 2, 3]\" }")
            } else {
                respond(content = "{ \"result\": \"OK\" }")
            }
        }
        val dataLoader = createDataLoader(
            localCache = localCache,
            remoteCacheExpiry = 1.hours,
            redisMockEngine = redisMockEngine,
        )

        dataLoader.load("key", sotOnly = true) {
            listOf(1, 2, 3, 4)
        }

        assertEquals(listOf(1, 2, 3, 4), localCache.getIfPresent("key"))
        assertEquals("key", redisMockEngine.requestHistory.last().url.segments.last())
        assertEquals("EX=${1.hours.inWholeSeconds}", redisMockEngine.requestHistory.last().url.encodedQuery)
        assertEquals(
            "TextContent[application/json] \"[1,2,3,4]\"",
            redisMockEngine.requestHistory.last().body.toString(),
        )
        assertEquals(HttpStatusCode.OK, redisMockEngine.responseHistory.last().statusCode)
    }

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
