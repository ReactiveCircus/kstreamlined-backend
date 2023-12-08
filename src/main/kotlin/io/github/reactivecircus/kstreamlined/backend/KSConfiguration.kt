package io.github.reactivecircus.kstreamlined.backend

import com.netflix.graphql.dgs.webflux.autoconfiguration.DgsWebfluxConfigurationProperties
import io.github.reactivecircus.kstreamlined.backend.client.ClientConfigs
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.client.KotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.client.RealFeedClient
import io.github.reactivecircus.kstreamlined.backend.client.RealKotlinWeeklyIssueClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(KStreamlinedRuntimeHints::class)
@RegisterReflectionForBinding(
    DgsWebfluxConfigurationProperties::class,
    DgsWebfluxConfigurationProperties.DgsWebsocketConfigurationProperties::class,
    DgsWebfluxConfigurationProperties.DgsGraphiQLConfigurationProperties::class,
    DgsWebfluxConfigurationProperties.DgsSchemaJsonConfigurationProperties::class,
)
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
