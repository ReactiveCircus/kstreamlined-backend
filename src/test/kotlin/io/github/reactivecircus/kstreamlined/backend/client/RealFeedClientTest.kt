package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeAuthor
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaCommunity
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaGroup
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
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

    private val mockKotlinYouTubeRssResponse =
        javaClass.classLoader.getResource("kotlin_youtube_rss_response_sample.xml")?.readText()!!

    private val mockTalkingKotlinRssResponse =
        javaClass.classLoader.getResource("talking_kotlin_rss_response_sample.xml")?.readText()!!

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

    @Test
    fun `loadKotlinYouTubeFeed() returns KotlinYouTubeItems when API call was successful`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinYouTubeRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        val expected = listOf(
            KotlinYouTubeItem(
                id = "yt:video:ihMhu3hvCCE",
                videoId = "ihMhu3hvCCE",
                channelId = "UCP7uiEZIqci43m22KDl0sNw",
                title = "Kotlin and Java Interoperability in Spring Projects",
                link = Link(
                    href = "https://www.youtube.com/watch?v=ihMhu3hvCCE",
                    rel = "alternate",
                ),
                author = KotlinYouTubeAuthor(
                    name = "Kotlin by JetBrains",
                    uri = "https://www.youtube.com/channel/UCP7uiEZIqci43m22KDl0sNw",
                ),
                published = "2022-07-06T13:39:46+00:00",
                updated = "2022-07-11T13:45:53+00:00",
                mediaGroup = MediaGroup(
                    title = "Kotlin and Java Interoperability in Spring Projects",
                    content = MediaGroup.Content(
                        url = "https://www.youtube.com/v/ihMhu3hvCCE?version=3",
                        type = "application/x-shockwave-flash",
                        width = "640",
                        height = "390",
                    ),
                    thumbnail = MediaGroup.Thumbnail(
                        url = "https://i2.ytimg.com/vi/ihMhu3hvCCE/hqdefault.jpg",
                        width = "480",
                        height = "360",
                    ),
                    description = "We have configured the Kotlin compiler in a Java/Spring project - now what? Let's talk about important details you need to know about calling Java from Kotlin code and vice versa. Links: Adding Kotlin to Spring/Maven project: https://youtu.be/4-qOxvjjF8g Calling Java from Kotlin: https://kotlinlang.org/docs/java-interop.html Calling Kotlin from Java: https://kotlinlang.org/docs/java-to-kotlin-interop.html Kotlin Spring compiler plugin: https://kotlinlang.org/docs/all-open-plugin.html#spring-support Just starting with Kotlin? Learn Kotlin by creating real-world applications with JetBrains Academy Build simple games, a chat bot, a coffee machine simulator, and other interactive projects step by step in a hands-on learning environment. Get started: https://hyperskill.org/join/fromyoutubetoJetSalesStat?redirect=true&next=/tracks/18 #springboot #springframework #kotlin #interoperability",
                    community = MediaCommunity(
                        starRating = MediaCommunity.StarRating(
                            count = "188",
                            average = "5.00",
                            min = "1",
                            max = "5",
                        ),
                        statistics = MediaCommunity.Statistics(views = "4397"),
                    ),
                ),
            ),
            KotlinYouTubeItem(
                id = "yt:video:tX4nLqcW2JA",
                videoId = "tX4nLqcW2JA",
                channelId = "UCP7uiEZIqci43m22KDl0sNw",
                title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta | Talking Kotlin",
                link = Link(
                    href = "https://www.youtube.com/watch?v=tX4nLqcW2JA",
                    rel = "alternate",
                ),
                author = KotlinYouTubeAuthor(
                    name = "Kotlin by JetBrains",
                    uri = "https://www.youtube.com/channel/UCP7uiEZIqci43m22KDl0sNw",
                ),
                published = "2022-06-28T15:00:15+00:00",
                updated = "2022-07-05T08:29:07+00:00",
                mediaGroup = MediaGroup(
                    title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta | Talking Kotlin",
                    content = MediaGroup.Content(
                        url = "https://www.youtube.com/v/tX4nLqcW2JA?version=3",
                        type = "application/x-shockwave-flash",
                        width = "640",
                        height = "390",
                    ),
                    thumbnail = MediaGroup.Thumbnail(
                        url = "https://i1.ytimg.com/vi/tX4nLqcW2JA/hqdefault.jpg",
                        width = "480",
                        height = "360",
                    ),
                    description = "We chat with Raul, Simon, and Alejandro to learn how Arrow adds functional paradigms and safety to Kotlin, and how it aims to influence the future of the language. Arrow - https://arrow-kt.io/ Arrow Analysis - https://arrow-kt.io/docs/meta/analysis/ 47 Degrees - https://www.47deg.com/ 47 Degrees jobs - https://www.47deg.com/company/ Arrow twitter - @arrow_kt #Arrow #Kotlin #Programming #arrow-kt",
                    community = MediaCommunity(
                        starRating = MediaCommunity.StarRating(
                            count = "124",
                            average = "5.00",
                            min = "1",
                            max = "5",
                        ),
                        statistics = MediaCommunity.Statistics(views = "3287"),
                    ),
                ),
            ),
        )

        assertEquals(expected, feedClient.loadKotlinYouTubeFeed())
    }

    @Test
    fun `loadKotlinYouTubeFeed() throws exception when API call failed`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        assertThrows<ClientRequestException> {
            feedClient.loadKotlinYouTubeFeed()
        }
    }

    @Test
    fun `loadTalkingKotlinFeed() returns TalkingYouTubeItems when API call was successful`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockTalkingKotlinRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/rss+xml")
            )
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        val expected = listOf(
            TalkingKotlinItem(
                id = "https://talkingkotlin.com/turbocharging-kotlin-arrow-analysis-optics-meta",
                title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta",
                link = Link(
                    href = "https://talkingkotlin.com/turbocharging-kotlin-arrow-analysis-optics-meta/",
                    rel = "alternate",
                    type = "text/html",
                    title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta",
                ),
                author = TalkingKotlinItem.Author(name = ""),
                published = "2022-06-28T00:00:00+02:00",
                updated = "2022-06-28T00:00:00+02:00",
                content = TalkingKotlinItem.Content(
                    type = "html",
                    base = "https://talkingkotlin.com/turbocharging-kotlin-arrow-analysis-optics-meta/",
                ),
                categories = listOf(
                    TalkingKotlinItem.Category("Arrow"),
                    TalkingKotlinItem.Category("Code Quality"),
                ),
                summary = TalkingKotlinItem.Summary("html"),
                thumbnail = TalkingKotlinItem.Thumbnail("https://talkingkotlin.com/arrow-analisys.png"),
                mediaContent = TalkingKotlinItem.MediaContent(
                    medium = "image",
                    url = "https://talkingkotlin.com/arrow-analisys.png",
                ),
            ),
            TalkingKotlinItem(
                id = "https://talkingkotlin.com/70-billion-events-per-day-adobe-and-kotlin",
                title = "70 Billion Events per Day – Adobe & Kotlin",
                link = Link(
                    href = "https://talkingkotlin.com/70-billion-events-per-day-adobe-and-kotlin/",
                    rel = "alternate",
                    type = "text/html",
                    title = "70 Billion Events per Day – Adobe & Kotlin",
                ),
                author = TalkingKotlinItem.Author(name = ""),
                published = "2022-04-19T00:00:00+02:00",
                updated = "2022-04-19T00:00:00+02:00",
                content = TalkingKotlinItem.Content(
                    type = "html",
                    base = "https://talkingkotlin.com/70-billion-events-per-day-adobe-and-kotlin/",
                ),
                categories = listOf(
                    TalkingKotlinItem.Category("Kotlin Multiplatform"),
                    TalkingKotlinItem.Category("Ktor"),
                    TalkingKotlinItem.Category("Adobe"),
                ),
                summary = TalkingKotlinItem.Summary("html"),
                thumbnail = TalkingKotlinItem.Thumbnail("https://talkingkotlin.com/Adobe.png"),
                mediaContent = TalkingKotlinItem.MediaContent(
                    medium = "image",
                    url = "https://talkingkotlin.com/Adobe.png",
                ),
            ),
        )

        assertEquals(expected, feedClient.loadTalkingKotlinFeed())
    }

    @Test
    fun `loadTalkingKotlinFeed() throws exception when API call failed`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedClient = RealFeedClient(mockEngine, TestClientConfigs)

        assertThrows<ClientRequestException> {
            feedClient.loadTalkingKotlinFeed()
        }
    }
}
