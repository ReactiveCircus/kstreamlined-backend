package io.github.reactivecircus.kstreamlined.backend.client

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogRss
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyRss
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeRss
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinRss
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.DefaultXml
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.decodeFromString
import org.apache.commons.text.StringEscapeUtils
import java.time.Duration

interface FeedClient {

    suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem>

    suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem>

    suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem>

    suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem>
}

class RealFeedClient(
    engine: HttpClientEngine,
    private val clientConfigs: ClientConfigs,
    private val kotlinBlogItemsCache: Cache<Unit, List<KotlinBlogItem>> = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build(),
    private val kotlinYouTubeItemsCache: Cache<Unit, List<KotlinYouTubeItem>> = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build(),
    private val talkingKotlinItemsCache: Cache<Unit, List<TalkingKotlinItem>> = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build(),
    private val kotlinWeeklyItemsCache: Cache<Unit, List<KotlinWeeklyItem>> = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build(),
) : FeedClient {

    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            xml(contentType = ContentType.Application.Rss)
            xml(contentType = ContentType.Application.Xml)
            xml(contentType = ContentType.Text.Xml)
        }
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> = getFromCacheOrFetch(kotlinBlogItemsCache) {
        httpClient.get(clientConfigs.kotlinBlogFeedUrl)
            .body<KotlinBlogRss>().channel.items.map {
                it.copy(
                    description = StringEscapeUtils.unescapeXml(it.description)
                )
            }
    }

    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> =
        getFromCacheOrFetch(kotlinYouTubeItemsCache) {
            httpClient.get(clientConfigs.kotlinYouTubeFeedUrl).bodyAsText().let {
                DefaultXml.decodeFromString<KotlinYouTubeRss>(it.replace("&(?!.{2,4};)".toRegex(), "&amp;")).entries
            }
        }

    override suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem> =
        getFromCacheOrFetch(talkingKotlinItemsCache) {
            httpClient.get(clientConfigs.talkingKotlinFeedUrl).body<TalkingKotlinRss>().entries
        }

    override suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem> = getFromCacheOrFetch(kotlinWeeklyItemsCache) {
        httpClient.get(clientConfigs.kotlinWeeklyFeedUrl).body<KotlinWeeklyRss>().channel.items.filter {
            it.creator.contains(KOTLIN_WEEKLY_TWITTER_USERNAME)
        }
    }

    companion object {
        private const val KOTLIN_WEEKLY_TWITTER_USERNAME = "@KotlinWeekly"
    }
}

private suspend fun <T : Any> getFromCacheOrFetch(cache: Cache<Unit, List<T>>, fetch: suspend () -> List<T>): List<T> {
    return cache.getIfPresent(Unit) ?: fetch().also {
        cache.put(Unit, it)
    }
}
