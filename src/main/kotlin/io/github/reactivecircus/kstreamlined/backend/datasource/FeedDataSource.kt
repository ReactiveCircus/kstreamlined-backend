package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogRss
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyRss
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeRss
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinRss
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.DefaultXml
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.DefaultXmlSerializationPolicy
import nl.adaptivity.xmlutil.serialization.XmlConfig
import org.apache.commons.text.StringEscapeUtils

interface FeedDataSource {
    suspend fun loadKotlinBlogFeed(skipCache: Boolean = false): List<KotlinBlogItem>
    suspend fun loadKotlinYouTubeFeed(skipCache: Boolean = false): List<KotlinYouTubeItem>
    suspend fun loadTalkingKotlinFeed(skipCache: Boolean = false): List<TalkingKotlinItem>
    suspend fun loadKotlinWeeklyFeed(skipCache: Boolean = false): List<KotlinWeeklyItem>
}

class FeedDataSourceConfig(
    val kotlinBlogFeedUrl: String,
    val kotlinYouTubeFeedUrl: String,
    val talkingKotlinFeedUrl: String,
    val kotlinWeeklyFeedUrl: String,
)

class RealFeedDataSource(
    engine: HttpClientEngine,
    private val dataSourceConfig: FeedDataSourceConfig,
    cacheConfig: DataLoader.CacheConfig,
    redisClient: RedisClient,
) : FeedDataSource {
    private val kotlinBlogFeedDataLoader = DataLoader.of(cacheConfig, redisClient, KotlinBlogItem.serializer())
    private val kotlinYouTubeFeedDataLoader = DataLoader.of(cacheConfig, redisClient, KotlinYouTubeItem.serializer())
    private val talkingKotlinFeedDataLoader = DataLoader.of(cacheConfig, redisClient, TalkingKotlinItem.serializer())
    private val kotlinWeeklyFeedDataLoader = DataLoader.of(cacheConfig, redisClient, KotlinWeeklyItem.serializer())

    @OptIn(ExperimentalXmlUtilApi::class)
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            val format = DefaultXml.copy {
                policy = DefaultXmlSerializationPolicy(
                    DefaultXmlSerializationPolicy.Builder().apply {
                        pedantic = false
                        unknownChildHandler = XmlConfig.IGNORING_UNKNOWN_CHILD_HANDLER
                    }.build()
                )
            }

            xml(format, ContentType.Application.Rss)
            xml(format, ContentType.Application.Xml)
            xml(format, ContentType.Text.Xml)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = HttpTimeoutMillis
            requestTimeoutMillis = HttpTimeoutMillis
        }
    }

    override suspend fun loadKotlinBlogFeed(skipCache: Boolean): List<KotlinBlogItem> {
        return kotlinBlogFeedDataLoader.load("kotlin-blog", sotOnly = skipCache) {
            httpClient.get(dataSourceConfig.kotlinBlogFeedUrl).body<KotlinBlogRss>().channel.items.map {
                it.copy(
                    description = StringEscapeUtils.unescapeXml(it.description).trim()
                )
            }
        }
    }

    override suspend fun loadKotlinYouTubeFeed(skipCache: Boolean): List<KotlinYouTubeItem> {
        return kotlinYouTubeFeedDataLoader.load("kotlin-youtube", sotOnly = skipCache) {
            httpClient.get(dataSourceConfig.kotlinYouTubeFeedUrl).bodyAsText().let {
                DefaultXml.decodeFromString<KotlinYouTubeRss>(it.replace("&(?!.{2,4};)".toRegex(), "&amp;")).entries
            }
        }
    }

    override suspend fun loadTalkingKotlinFeed(skipCache: Boolean): List<TalkingKotlinItem> {
        return talkingKotlinFeedDataLoader.load("talking-kotlin", sotOnly = skipCache) {
            httpClient.get(dataSourceConfig.talkingKotlinFeedUrl).body<TalkingKotlinRss>().channel.items
                .take(TalkingKotlinFeedSize)
                .map {
                    it.copy(
                        summary = StringEscapeUtils.unescapeXml(it.summary).trim()
                    )
                }
        }
    }

    override suspend fun loadKotlinWeeklyFeed(skipCache: Boolean): List<KotlinWeeklyItem> {
        return kotlinWeeklyFeedDataLoader.load("kotlin-weekly", sotOnly = skipCache) {
            httpClient.get(dataSourceConfig.kotlinWeeklyFeedUrl).body<KotlinWeeklyRss>().channel.items
        }
    }

    companion object {
        private const val HttpTimeoutMillis = 30_000L
        private const val TalkingKotlinFeedSize = 10
    }
}
