package io.github.reactivecircus.kstreamlined.backend

import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.client.ClientConfigs
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.client.RealFeedClient
import io.ktor.client.engine.cio.CIO
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

@Configuration
class KSConfiguration {

    @Bean
    fun feedClient(clientConfigs: ClientConfigs): FeedClient {
        return RealFeedClient(
            engine = CIO.create(),
            clientConfigs = clientConfigs,
            kotlinBlogItemsCache = Caffeine
                .newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .build(),
            kotlinYouTubeItemsCache = Caffeine
                .newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .build(),
            talkingKotlinItemsCache = Caffeine
                .newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .build(),
            kotlinWeeklyItemsCache = Caffeine
                .newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .build(),
        )
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
