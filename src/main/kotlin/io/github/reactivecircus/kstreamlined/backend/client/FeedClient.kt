package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.kotlinblog.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.kotlinblog.KotlinBlogRss
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.xml.xml
import org.apache.commons.text.StringEscapeUtils

interface FeedClient {
    suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem>
}

class RealFeedClient(
    engine: HttpClientEngine,
    private val clientConfigs: ClientConfigs,
) : FeedClient {

    private val httpClient = HttpClient(engine) {
        install(ContentNegotiation) {
            xml(
                contentType = ContentType.Application.Rss,
            )
        }
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return httpClient.get(clientConfigs.kotlinBlogFeedUrl).body<KotlinBlogRss>().channel.items.map {
            it.copy(
                description = StringEscapeUtils.unescapeXml(it.description)
            )
        }
    }
}
