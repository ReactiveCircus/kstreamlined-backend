package io.github.reactivecircus.kstreamlined.backend.client

import com.github.benmanes.caffeine.cache.Cache
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
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import org.apache.commons.text.StringEscapeUtils

interface FeedClient {

    context(CacheContext<KotlinBlogItem>)
    suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem>

    context(CacheContext<KotlinYouTubeItem>)
    suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem>

    context(CacheContext<TalkingKotlinItem>)
    suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem>

    context(CacheContext<KotlinWeeklyItem>)
    suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem>
}

interface CacheContext<T : Any> {
    val cache: Cache<Unit, List<T>>
}

class RealFeedClient(
    engine: HttpClientEngine,
    private val clientConfigs: ClientConfigs
) : FeedClient {

    @OptIn(ExperimentalXmlUtilApi::class)
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            val format = DefaultXml.copy {
                policy = DefaultXmlSerializationPolicy(
                    pedantic = false,
                    unknownChildHandler = { _, _, _, _, _ -> emptyList() }
                )
            }

            xml(format, ContentType.Application.Rss)
            xml(format, ContentType.Application.Xml)
            xml(format, ContentType.Text.Xml)
        }
    }

    context(CacheContext<KotlinBlogItem>)
    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> = getFromCacheOrFetch {
        httpClient.get(clientConfigs.kotlinBlogFeedUrl)
            .body<KotlinBlogRss>().channel.items.map {
                it.copy(
                    description = StringEscapeUtils.unescapeXml(it.description)
                )
            }
    }

    context(CacheContext<KotlinYouTubeItem>)
    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> = getFromCacheOrFetch {
        httpClient.get(clientConfigs.kotlinYouTubeFeedUrl).bodyAsText().let {
            DefaultXml.decodeFromString<KotlinYouTubeRss>(it.replace("&(?!.{2,4};)".toRegex(), "&amp;")).entries
        }
    }

    context(CacheContext<TalkingKotlinItem>)
    override suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem> = getFromCacheOrFetch {
        httpClient.get(clientConfigs.talkingKotlinFeedUrl).body<TalkingKotlinRss>().entries
    }

    context(CacheContext<KotlinWeeklyItem>)
    override suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem> = getFromCacheOrFetch {
        httpClient.get(clientConfigs.kotlinWeeklyFeedUrl).body<KotlinWeeklyRss>().channel.items.filter {
            it.creator.contains(KOTLIN_WEEKLY_TWITTER_USERNAME)
        }
    }

    companion object {
        private const val KOTLIN_WEEKLY_TWITTER_USERNAME = "@KotlinWeekly"
    }
}

context(CacheContext<T>)
private suspend fun <T : Any> getFromCacheOrFetch(fetch: suspend () -> List<T>): List<T> {
    return cache.getIfPresent(Unit) ?: fetch().also {
        cache.put(Unit, it)
    }
}
