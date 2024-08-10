package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.client.ClientConfigs
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.client.KotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.client.RealFeedClient
import io.github.reactivecircus.kstreamlined.backend.client.RealKotlinWeeklyIssueClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KSConfiguration {

    @Bean
    fun feedClient(
        engine: HttpClientEngine,
        clientConfigs: ClientConfigs,
    ): FeedClient {
        return RealFeedClient(
            engine = engine,
            clientConfigs = clientConfigs,
        )
    }

    @Bean
    fun kotlinWeeklyIssueClient(
        engine: HttpClientEngine
    ): KotlinWeeklyIssueClient {
        return RealKotlinWeeklyIssueClient(
            engine = engine,
        )
    }

    @Bean
    fun httpClientEngine(): HttpClientEngine {
        return CIO.create()
    }

    @Bean
    fun clientConfigs(
        @Value("\${ks.kotlin-blog-feed-url}") kotlinBlogFeedUrl: String,
        @Value("\${ks.kotlin-youtube-feed-url}") kotlinYouTubeFeedUrl: String,
        @Value("\${ks.talking-kotlin-feed-url}") talkingKotlinFeedUrl: String,
        @Value("\${ks.kotlin-weekly-feed-url}") kotlinWeeklyFeedUrl: String,
    ): ClientConfigs {
        return ClientConfigs(
            kotlinBlogFeedUrl = kotlinBlogFeedUrl,
            kotlinYouTubeFeedUrl = kotlinYouTubeFeedUrl,
            talkingKotlinFeedUrl = talkingKotlinFeedUrl,
            kotlinWeeklyFeedUrl = kotlinWeeklyFeedUrl,
        )
    }
}
