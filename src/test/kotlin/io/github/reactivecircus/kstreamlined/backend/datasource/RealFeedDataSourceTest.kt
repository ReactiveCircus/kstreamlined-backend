package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeAuthor
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.Link
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.MediaCommunity
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.MediaGroup
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.time.Duration.Companion.seconds

class RealFeedDataSourceTest {

    private val mockKotlinBlogRssResponse =
        javaClass.classLoader.getResource("kotlin_blog_rss_response_sample.xml")?.readText()!!

    private val mockKotlinYouTubeRssResponse =
        javaClass.classLoader.getResource("kotlin_youtube_rss_response_sample.xml")?.readText()!!

    private val mockTalkingKotlinRssResponse =
        javaClass.classLoader.getResource("talking_kotlin_rss_response_sample.xml")?.readText()!!

    private val mockKotlinWeeklyRssResponse =
        javaClass.classLoader.getResource("kotlin_weekly_rss_response_sample.xml")?.readText()!!

    private val cacheConfig = DataLoader.CacheConfig(
        localExpiry = 0.seconds,
        remoteExpiry = 0.seconds,
    )

    private val feedPersister = FakeFeedPersister()

    @Test
    fun `loadKotlinBlogFeed() returns KotlinBlogItems when API call succeeds`() = runBlocking {
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

        val expected = listOf(
            KotlinBlogItem(
                title = "A New Approach to Incremental Compilation in Kotlin",
                link = "https://blog.jetbrains.com/kotlin/2022/07/a-new-approach-to-incremental-compilation-in-kotlin/",
                pubDate = "Fri, 15 Jul 2022 10:56:13 +0000",
                featuredImage = "https://blog.jetbrains.com/wp-content/uploads/2022/07/A-New-Approach-to-Incremental-Compilation-in-Kotlin-EN-2_Twitter-Blog.png",
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=264203",
                description = "In Kotlin 1.7.0, we’ve reworked incremental compilation for project changes in cross-module dependencies. The new approach lifts previous limitations on incremental compilation. It’s now supported when changes are made inside dependent non-Kotlin modules, and it is compatible with the Gradle build cache. Support for compilation avoidance has also been improved. All of these advancements decrease […]",
            ),
            KotlinBlogItem(
                title = "Kotlin News: KotlinConf, Build Reports, DataFrame Preview, and More",
                link = "https://blog.jetbrains.com/kotlin/2022/07/kotlin-news-june/",
                pubDate = "Fri, 15 Jul 2022 10:32:08 +0000",
                featuredImage = "https://blog.jetbrains.com/wp-content/uploads/2022/07/Monthly-digest-4-Summer-2022-01.png",
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=265263",
                description = "Kotlin Developer Survey is Open Share your opinions about the language itself, IDEs, libraries, build tools, and subsystems. Your answers will help the Kotlin team make the language and tools even better and more convenient. Give your feedback KotlinConf is Back! KotlinConf is making a glorious comeback! It will take place in Amsterdam on April […]",
            ),
        )

        assert(feedDataSource.loadKotlinBlogFeed() == expected)
        assert(feedPersister.loadKotlinBlogItems() == expected)
    }

    @Test
    fun `loadKotlinBlogFeed() throws exception when API call fails`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assertFailsWith<ClientRequestException> {
            feedDataSource.loadKotlinBlogFeed()
        }
    }

    @Test
    fun `loadKotlinYouTubeFeed() returns KotlinYouTubeItems when API call succeeds`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinYouTubeRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "application/xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

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

        assert(feedDataSource.loadKotlinYouTubeFeed() == expected)
        assert(feedPersister.loadKotlinYouTubeItems() == expected)
    }

    @Test
    fun `loadKotlinYouTubeFeed() throws exception when API call fails`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assertFailsWith<ClientRequestException> {
            feedDataSource.loadKotlinYouTubeFeed()
        }
    }

    @Test
    fun `loadTalkingKotlinFeed() returns TalkingKotlinItems when API call succeeds`() = runBlocking {
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

        val expected = listOf(
            TalkingKotlinItem(
                guid = "tag:soundcloud,2010:tracks/1295949565",
                title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta",
                link = "https://soundcloud.com/user-38099918/arrow-analysis",
                pubDate = "Tue, 28 Jun 2022 16:00:27 +0000",
                summary = "We chat with Raul, Simon, and Alejandro to learn how Arrow adds functional paradigms and safety to Kotlin, and how it aims to influence the future of the language.",
                duration = "00:57:44",
                enclosure = TalkingKotlinItem.Enclosure(url = "https://feeds.soundcloud.com/stream/1295949565-user-38099918-arrow-analysis.mp3"),
                image = TalkingKotlinItem.Image(href = "https://i1.sndcdn.com/artworks-yEP8SdbEZOJcmVay-AWlLHQ-t3000x3000.jpg"),
            ),
            TalkingKotlinItem(
                guid = "tag:soundcloud,2010:tracks/1253069788",
                title = "70 Billion Events per Day – Adobe & Kotlin",
                link = "https://soundcloud.com/user-38099918/70-billion-events-per-day-adobe-kotlin",
                pubDate = "Tue, 19 Apr 2022 16:00:24 +0000",
                summary = "We talked to Rares Vlasceanu and Catalin Costache from Adobe about how they handle 70 000 000 000 events per day with the help of Kotlin and Ktor.",
                duration = "00:51:09",
                enclosure = TalkingKotlinItem.Enclosure(url = "https://feeds.soundcloud.com/stream/1253069788-user-38099918-70-billion-events-per-day-adobe-kotlin.mp3"),
                image = TalkingKotlinItem.Image(href = "https://i1.sndcdn.com/artworks-ANd7JtyjHYkhAUsQ-XU8I0Q-t3000x3000.jpg"),
            ),
        )

        assert(feedDataSource.loadTalkingKotlinFeed() == expected)
        assert(feedPersister.loadTalkingKotlinItems() == expected)
    }

    @Test
    fun `loadTalkingKotlinFeed() throws exception when API call fails`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assertFailsWith<ClientRequestException> {
            feedDataSource.loadTalkingKotlinFeed()
        }
    }

    @Test
    fun `loadKotlinWeeklyFeed() returns KotlinWeeklyItems when API call succeeds`() = runBlocking {
        val mockEngine = MockEngine {
            respond(
                content = ByteReadChannel(mockKotlinWeeklyRssResponse),
                headers = headersOf(HttpHeaders.ContentType, "text/xml")
            )
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        val expected = listOf(
            KotlinWeeklyItem(
                title = "Kotlin Weekly #381",
                link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
                guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
                pubDate = "Sun, 19 Nov 2023 09:13:00 +0000",
            ),
            KotlinWeeklyItem(
                title = "Kotlin Weekly #380",
                link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-380",
                guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-380",
                pubDate = "Sun, 12 Nov 2023 09:14:59 +0000",
            ),
        )

        assert(feedDataSource.loadKotlinWeeklyFeed() == expected)
        assert(feedPersister.loadKotlinWeeklyItems() == expected)
    }

    @Test
    fun `loadKotlinWeeklyFeed() throws exception when API call fails`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val feedDataSource = RealFeedDataSource(
            engine = mockEngine,
            dataSourceConfig = TestFeedDataSourceConfig,
            cacheConfig = cacheConfig,
            redisClient = NoOpRedisClient,
            feedPersister = feedPersister,
        )

        assertFailsWith<ClientRequestException> {
            feedDataSource.loadKotlinWeeklyFeed()
        }
    }
}
