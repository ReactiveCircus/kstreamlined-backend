package io.github.reactivecircus.kstreamlined.backend.service

import io.github.reactivecircus.kstreamlined.backend.store.FakeFeedStore
import io.github.reactivecircus.kstreamlined.backend.store.FakeKotlinBlogContentStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class FullResponseParserTest {
    private val mockKotlinBlogRssResponse =
        javaClass.classLoader.getResource("kotlin_blog_rss_response_full.xml")?.readText()!!

    private val mockKotlinYouTubeRssResponse =
        javaClass.classLoader.getResource("kotlin_youtube_rss_response_full.xml")?.readText()!!

    private val mockTalkingKotlinRssResponse =
        javaClass.classLoader.getResource("talking_kotlin_rss_response_full.xml")?.readText()!!

    private val mockKotlinWeeklyRssResponse =
        javaClass.classLoader.getResource("kotlin_weekly_rss_response_full.xml")?.readText()!!

    private val cacheConfig = DataLoader.CacheConfig(
        localExpiry = 0.seconds,
        remoteExpiry = 0.seconds,
    )

    private val feedStore = FakeFeedStore()

    private val kotlinBlogContentStore = FakeKotlinBlogContentStore()

    @Test
    fun `can parse Kotlin Blog RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinBlogRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml"),
            )
        }
        val feedService = RealFeedService(
            engine = mockEngine,
            serviceConfig = TestFeedServiceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedStore = feedStore,
            kotlinBlogContentStore = kotlinBlogContentStore,
        )

        assertEquals(12, feedService.loadKotlinBlogFeed().size)
        assertEquals(12, kotlinBlogContentStore.allKotlinBlogContents.size)
    }

    @Test
    fun `can parse Kotlin YouTube RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinYouTubeRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml"),
            )
        }
        val feedService = RealFeedService(
            engine = mockEngine,
            serviceConfig = TestFeedServiceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedStore = feedStore,
            kotlinBlogContentStore = kotlinBlogContentStore,
        )

        assertEquals(15, feedService.loadKotlinYouTubeFeed().size)
    }

    @Test
    fun `can parse Talking Kotlin RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockTalkingKotlinRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml"),
            )
        }
        val feedService = RealFeedService(
            engine = mockEngine,
            serviceConfig = TestFeedServiceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedStore = feedStore,
            kotlinBlogContentStore = kotlinBlogContentStore,
        )

        assertEquals(10, feedService.loadTalkingKotlinFeed().size)
    }

    @Test
    fun `can parse Kotlin Weekly RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinWeeklyRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml"),
            )
        }
        val feedService = RealFeedService(
            engine = mockEngine,
            serviceConfig = TestFeedServiceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedStore = feedStore,
            kotlinBlogContentStore = kotlinBlogContentStore,
        )

        assertEquals(3, feedService.loadKotlinWeeklyFeed().size)
    }
}
