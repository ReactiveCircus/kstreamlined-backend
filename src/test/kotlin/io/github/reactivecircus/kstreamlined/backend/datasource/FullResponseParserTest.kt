package io.github.reactivecircus.kstreamlined.backend.datasource

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
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

    private val feedPersister = FakeFeedPersister()

    @Test
    fun `can parse Kotlin Blog RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinBlogRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assert(feedDataSource.loadKotlinBlogFeed().size == 12)
    }

    @Test
    fun `can parse Kotlin YouTube RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinYouTubeRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assert(feedDataSource.loadKotlinYouTubeFeed().size == 15)
    }

    @Test
    fun `can parse Talking Kotlin RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockTalkingKotlinRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assert(feedDataSource.loadTalkingKotlinFeed().size == 10)
    }

    @Test
    fun `can parse Kotlin Weekly RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinWeeklyRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assert(feedDataSource.loadKotlinWeeklyFeed().size == 3)
    }
}
