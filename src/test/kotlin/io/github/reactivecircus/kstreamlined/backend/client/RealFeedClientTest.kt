package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class RealFeedClientTest {

    private val mockKotlinBlogRssResponse =
        javaClass.classLoader.getResource("kotlin_blog_rss_response_sample.xml")?.readText()!!

    @Test
    fun `loadKotlinBlogFeed() returns KotlinBlogItems when API call was successful`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinBlogRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        val expected = listOf(
            KotlinBlogItem(
                title = "A New Approach to Incremental Compilation in Kotlin",
                link = "https://blog.jetbrains.com/kotlin/2022/07/a-new-approach-to-incremental-compilation-in-kotlin/",
                creator = "Andrey Uskov",
                pubDate = "Fri, 15 Jul 2022 10:56:13 +0000",
                featuredImage = "https://blog.jetbrains.com/wp-content/uploads/2022/07/A-New-Approach-to-Incremental-Compilation-in-Kotlin-EN-2_Twitter-Blog.png",
                categories = listOf("news"),
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=264203",
                description = "In Kotlin 1.7.0, we’ve reworked incremental compilation for project changes in cross-module dependencies. The new approach lifts previous limitations on incremental compilation. It’s now supported when changes are made inside dependent non-Kotlin modules, and it is compatible with the Gradle build cache. Support for compilation avoidance has also been improved. All of these advancements decrease […]",
                encoded = "<p>In Kotlin 1.7.0, we&#8217;ve reworked incremental compilation for project changes in cross-module dependencies. The new approach lifts previous limitations on incremental compilation. It’s now supported when changes are made inside dependent non-Kotlin modules, and it is compatible with the <a href=\"https://docs.gradle.org/current/userguide/build_cache.html\">Gradle build cache</a>. Support for compilation avoidance has also been improved. All of these advancements decrease the number of necessary full-module and file recompilations, making the overall compilation time faster.</p>",
                languages = emptyList(),
            ),
            KotlinBlogItem(
                title = "Kotlin News: KotlinConf, Build Reports, DataFrame Preview, and More",
                link = "https://blog.jetbrains.com/kotlin/2022/07/kotlin-news-june/",
                creator = "Ekaterina Petrova",
                pubDate = "Fri, 15 Jul 2022 10:32:08 +0000",
                featuredImage = "https://blog.jetbrains.com/wp-content/uploads/2022/07/Monthly-digest-4-Summer-2022-01.png",
                categories = listOf("news", "kotlin-news", "newsletter"),
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=265263",
                description = "Kotlin Developer Survey is Open Share your opinions about the language itself, IDEs, libraries, build tools, and subsystems. Your answers will help the Kotlin team make the language and tools even better and more convenient. Give your feedback KotlinConf is Back! KotlinConf is making a glorious comeback! It will take place in Amsterdam on April […]",
                encoded = "<h2><strong>Kotlin Developer Survey is Open</strong></h2>",
                languages = listOf(
                    KotlinBlogItem.Language(
                        code = "zh-hans",
                        url = "https://blog.jetbrains.com/zh-hans/kotlin/2022/06/multiplatform-survey-q3-q4-2021/",
                    ),
                    KotlinBlogItem.Language(
                        code = "fr",
                        url = "https://blog.jetbrains.com/fr/kotlin/2022/06/multiplatform-survey-q3-q4-2021/",
                    ),
                ),
            ),
        )

        assertEquals(expected, feedClient.loadKotlinBlogFeed())
    }

    @Test
    fun `loadKotlinBlogFeed() throws exception when API call failed`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        assertThrows<ClientRequestException> {
            feedClient.loadKotlinBlogFeed()
        }
    }
}
