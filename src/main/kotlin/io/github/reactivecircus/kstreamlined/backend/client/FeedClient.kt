package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogRss
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeRss
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

interface FeedClient {
    suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem>
    suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem>
}

class RealFeedClient(
    engine: HttpClientEngine,
    private val clientConfigs: ClientConfigs,
) : FeedClient {

    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            xml(contentType = ContentType.Application.Rss)
        }
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return httpClient.get(clientConfigs.kotlinBlogFeedUrl).body<KotlinBlogRss>().channel.items.map {
            it.copy(
                description = StringEscapeUtils.unescapeXml(it.description)
            )
        }
    }

    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> {
        return httpClient.get(clientConfigs.kotlinYouTubeFeedUrl).bodyAsText().let {
            DefaultXml.decodeFromString<KotlinYouTubeRss>(it.replace("&(?!.{2,4};)".toRegex(), "&amp;")).entries
        }
    }
}
