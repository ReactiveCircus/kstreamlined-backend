package io.github.reactivecircus.kstreamlined.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.datasource.DataLoader
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSourceConfig
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinBlogTldrDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.RealFeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.RealKotlinBlogTldrDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.RealKotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FeedPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FirestoreFeedPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FirestoreKotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FirestoreKotlinBlogTldrPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrPersister
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
        feedPersister: FeedPersister,
        kotlinBlogContentPersister: KotlinBlogContentPersister,
    ): FeedDataSource {
        return RealFeedDataSource(
            engine = engine,
            dataSourceConfig = dataSourceConfig,
            cacheConfig = DataLoader.CacheConfig(
                localExpiry = 10.minutes,
                remoteExpiry = 1.hours,
            ),
            redisClient = redisClient,
            feedPersister = feedPersister,
            kotlinBlogContentPersister = kotlinBlogContentPersister,
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
    fun feedPersister(
        firestore: Firestore,
    ): FeedPersister {
        return FirestoreFeedPersister(firestore = firestore)
    }

    @Bean
    fun kotlinBlogContentPersister(
        firestore: Firestore,
    ): KotlinBlogContentPersister {
        return FirestoreKotlinBlogContentPersister(firestore = firestore)
    }

    @Bean
    fun kotlinBlogTldrPersister(
        firestore: Firestore,
    ): KotlinBlogTldrPersister {
        return FirestoreKotlinBlogTldrPersister(firestore = firestore)
    }

    @Bean
    fun kotlinBlogTldrDataSource(
        kotlinBlogContentPersister: KotlinBlogContentPersister,
    ): KotlinBlogTldrDataSource {
        return RealKotlinBlogTldrDataSource(kotlinBlogContentPersister = kotlinBlogContentPersister)
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

    @Bean
    fun cloudflareAiClient(
        engine: HttpClientEngine,
        @Value("\${KS_CF_BASE_URL}") baseUrl: String,
        @Value("\${KS_CF_ACCOUNT_ID}") accountId: String,
        @Value("\${KS_CF_API_TOKEN}") apiToken: String,
    ): CloudflareAiClient {
        return CloudflareAiClient(
            engine = engine,
            baseUrl = baseUrl,
            accountId = accountId,
            apiToken = apiToken,
        )
    }

    @Bean
    fun firestore(
        @Value("\${KS_GCLOUD_PROJECT_ID}") projectId: String,
    ): Firestore {
        val firestoreOptions = FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        return firestoreOptions.service
    }
}
