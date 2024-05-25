package io.github.reactivecircus.kstreamlined.backend.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class FullResponseParserTest {

    private val mockKotlinBlogRssResponse =
        javaClass.classLoader.getResource("kotlin_blog_rss_response_full.xml")?.readText()!!

    private val mockKotlinYouTubeRssResponse =
        javaClass.classLoader.getResource("kotlin_youtube_rss_response_full.xml")?.readText()!!

    private val mockTalkingKotlinRssResponse =
        javaClass.classLoader.getResource("talking_kotlin_rss_response_full.xml")?.readText()!!

    private val mockKotlinWeeklyRssResponse =
        javaClass.classLoader.getResource("kotlin_weekly_rss_response_full.xml")?.readText()!!

    @Test
    fun `can parse Kotlin Blog RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinBlogRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        with(fakeKotlinBlogCacheContext()) {
            assert(feedClient.loadKotlinBlogFeed().size == 12)
        }
    }

    @Test
    fun `can parse Kotlin YouTube RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinYouTubeRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        with(fakeKotlinYouTubeCacheContext()) {
            assert(feedClient.loadKotlinYouTubeFeed().size == 15)
        }
    }

    @Test
    fun `can parse Talking Kotlin RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockTalkingKotlinRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        with(fakeTalkingKotlinCacheContext()) {
            assert(feedClient.loadTalkingKotlinFeed().size == 10)
        }
    }

    @Test
    fun `can parse Kotlin Weekly RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinWeeklyRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        with(fakeKotlinWeeklyCacheContext()) {
            assert(feedClient.loadKotlinWeeklyFeed().size == 3)
        }
    }
}
