package io.github.reactivecircus.kstreamlined.backend.client

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class FullResponseParserTest {

    private val mockKotlinBlogRssResponse =
        javaClass.classLoader.getResource("kotlin_blog_rss_response_full.xml")?.readText()!!

    private val mockKotlinYouTubeRssResponse =
        javaClass.classLoader.getResource("kotlin_youtube_rss_response_full.xml")?.readText()!!

    private val mockTalkingKotlinRssResponse =
        javaClass.classLoader.getResource("talking_kotlin_rss_response_full.xml")?.readText()!!

    @Test
    fun `can parse Kotlin Blog RSS feed`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinBlogRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        assertEquals(12, feedClient.loadKotlinBlogFeed().size)
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

        assertEquals(15, feedClient.loadKotlinYouTubeFeed().size)
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

        assertEquals(10, feedClient.loadTalkingKotlinFeed().size)
    }
}
