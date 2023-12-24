package io.github.reactivecircus.kstreamlined.backend.client

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeAuthor
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaCommunity
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaGroup
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem

object FakeFeedClient : FeedClient {

    var nextKotlinBlogFeedResponse: () -> List<KotlinBlogItem> = {
        listOf(DummyKotlinBlogItem)
    }

    var nextKotlinYouTubeFeedResponse: () -> List<KotlinYouTubeItem> = {
        listOf(DummyKotlinYouTubeItem)
    }

    var nextTalkingKotlinFeedResponse: () -> List<TalkingKotlinItem> = {
        listOf(DummyTalkingKotlinItem)
    }

    var nextKotlinWeeklyFeedResponse: () -> List<KotlinWeeklyItem> = {
        listOf(DummyKotlinWeeklyItem)
    }

    context(CacheContext<KotlinBlogItem>)
    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return nextKotlinBlogFeedResponse()
    }

    context(CacheContext<KotlinYouTubeItem>)
    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> {
        return nextKotlinYouTubeFeedResponse()
    }

    context(CacheContext<TalkingKotlinItem>)
    override suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem> {
        return nextTalkingKotlinFeedResponse()
    }

    context(CacheContext<KotlinWeeklyItem>)
    override suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem> {
        return nextKotlinWeeklyFeedResponse()
    }
}

fun fakeKotlinBlogCacheContext() = object : CacheContext<KotlinBlogItem> {
    override val cache: Cache<Unit, List<KotlinBlogItem>> = Caffeine.newBuilder().build()
}

fun fakeKotlinYouTubeCacheContext() = object : CacheContext<KotlinYouTubeItem> {
    override val cache: Cache<Unit, List<KotlinYouTubeItem>> = Caffeine.newBuilder().build()
}

fun fakeTalkingKotlinCacheContext() = object : CacheContext<TalkingKotlinItem> {
    override val cache: Cache<Unit, List<TalkingKotlinItem>> = Caffeine.newBuilder().build()
}

fun fakeKotlinWeeklyCacheContext() = object : CacheContext<KotlinWeeklyItem> {
    override val cache: Cache<Unit, List<KotlinWeeklyItem>> = Caffeine.newBuilder().build()
}

val DummyKotlinBlogItem = KotlinBlogItem(
    title = "A New Approach to Incremental Compilation in Kotlin",
    link = "https://blog.jetbrains.com/kotlin/2022/07/a-new-approach-to-incremental-compilation-in-kotlin/",
    pubDate = "Fri, 15 Jul 2022 10:56:13 +0000",
    featuredImage = "https://blog.jetbrains.com/wp-content/uploads/2022/07/A-New-Approach-to-Incremental-Compilation-in-Kotlin-EN-2_Twitter-Blog.png",
    guid = "https://blog.jetbrains.com/?post_type=kotlin&p=264203",
    description = "In Kotlin 1.7.0, we’ve reworked incremental compilation for project changes in cross-module dependencies. The new approach lifts previous limitations on incremental compilation. It’s now supported when changes are made inside dependent non-Kotlin modules, and it is compatible with the Gradle build cache. Support for compilation avoidance has also been improved. All of these advancements decrease […]",
)

val DummyKotlinYouTubeItem = KotlinYouTubeItem(
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
)

val DummyTalkingKotlinItem = TalkingKotlinItem(
    guid = "tag:soundcloud,2010:tracks/1295949565",
    title = "Turbocharging Kotlin: Arrow Analysis, Optics & Meta",
    link = "https://soundcloud.com/user-38099918/arrow-analysis",
    pubDate = "Tue, 28 Jun 2022 16:00:27 +0000",
    summary = "We chat with Raul, Simon, and Alejandro to learn how Arrow adds functional paradigms and safety to Kotlin, and how it aims to influence the future of the language.",
    duration = "00:57:44",
    image = TalkingKotlinItem.Image(href = "https://i1.sndcdn.com/artworks-yEP8SdbEZOJcmVay-AWlLHQ-t3000x3000.jpg"),
)

val DummyKotlinWeeklyItem = KotlinWeeklyItem(
    title = "Kotlin Weekly #381",
    link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
    guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
    pubDate = "Sun, 19 Nov 2023 09:13:00 +0000",
)
