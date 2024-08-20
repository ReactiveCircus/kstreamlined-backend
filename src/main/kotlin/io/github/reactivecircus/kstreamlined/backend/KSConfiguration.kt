package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.datasource.DataLoader
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSourceConfig
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.RealFeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.RealKotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Configuration
class KSConfiguration {

    @Bean
    fun feedDataSource(
        engine: HttpClientEngine,
        dataSourceConfig: FeedDataSourceConfig,
        redisClient: RedisClient,
    ): FeedDataSource {
        return RealFeedDataSource(
            engine = engine,
            dataSourceConfig = dataSourceConfig,
            cacheConfig = DataLoader.CacheConfig(
                localExpiry = 10.minutes,
                remoteExpiry = 1.hours,
            ),
            redisClient = redisClient,
        )
    }

    @Bean
    fun feedDataSourceConfig(
        @Value("\${ks.kotlin-blog-feed-url}") kotlinBlogFeedUrl: String,
        @Value("\${ks.kotlin-youtube-feed-url}") kotlinYouTubeFeedUrl: String,
        @Value("\${ks.talking-kotlin-feed-url}") talkingKotlinFeedUrl: String,
        @Value("\${ks.kotlin-weekly-feed-url}") kotlinWeeklyFeedUrl: String,
    ): FeedDataSourceConfig {
        return FeedDataSourceConfig(
            kotlinBlogFeedUrl = kotlinBlogFeedUrl,
            kotlinYouTubeFeedUrl = kotlinYouTubeFeedUrl,
            talkingKotlinFeedUrl = talkingKotlinFeedUrl,
            kotlinWeeklyFeedUrl = kotlinWeeklyFeedUrl,
        )
    }

    @Bean
    fun kotlinWeeklyIssueDataSource(
        engine: HttpClientEngine
    ): KotlinWeeklyIssueDataSource {
        return RealKotlinWeeklyIssueDataSource(
            engine = engine,
        )
    }

    @Bean
    fun httpClientEngine(): HttpClientEngine {
        return OkHttp.create()
    }

    @Bean
    fun redisClient(
        engine: HttpClientEngine,
        @Value("\${KS_REDIS_REST_URL}") redisUrl: String,
        @Value("\${KS_REDIS_REST_TOKEN}") redisToken: String,
    ): RedisClient {
        return RedisClient(
            engine = engine,
            url = redisUrl,
            token = redisToken,
        )
    }
}
