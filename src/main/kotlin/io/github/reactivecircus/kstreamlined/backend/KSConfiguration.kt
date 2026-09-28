package io.github.reactivecircus.kstreamlined.backend

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.github.reactivecircus.kstreamlined.backend.service.DataLoader
import io.github.reactivecircus.kstreamlined.backend.service.FeedService
import io.github.reactivecircus.kstreamlined.backend.service.FeedServiceConfig
import io.github.reactivecircus.kstreamlined.backend.service.KotlinBlogTldrService
import io.github.reactivecircus.kstreamlined.backend.service.KotlinWeeklyIssueService
import io.github.reactivecircus.kstreamlined.backend.service.RealFeedService
import io.github.reactivecircus.kstreamlined.backend.service.RealKotlinBlogTldrService
import io.github.reactivecircus.kstreamlined.backend.service.RealKotlinWeeklyIssueService
import io.github.reactivecircus.kstreamlined.backend.store.FeedStore
import io.github.reactivecircus.kstreamlined.backend.store.FirestoreFeedStore
import io.github.reactivecircus.kstreamlined.backend.store.FirestoreKotlinBlogContentStore
import io.github.reactivecircus.kstreamlined.backend.store.KotlinBlogContentStore
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
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
    fun feedService(
        engine: HttpClientEngine,
        serviceConfig: FeedServiceConfig,
        redisClient: RedisClient,
        feedStore: FeedStore,
        kotlinBlogContentStore: KotlinBlogContentStore,
    ): FeedService {
        return RealFeedService(
            engine = engine,
            serviceConfig = serviceConfig,
            cacheConfig = DataLoader.CacheConfig(
                localExpiry = 10.minutes,
                remoteExpiry = 1.hours,
            ),
            redisClient = redisClient,
            feedStore = feedStore,
            kotlinBlogContentStore = kotlinBlogContentStore,
        )
    }

    @Bean
    fun feedServiceConfig(
        @Value("\${ks.kotlin-blog-feed-url}") kotlinBlogFeedUrl: String,
        @Value("\${ks.kotlin-youtube-feed-url}") kotlinYouTubeFeedUrl: String,
        @Value("\${ks.talking-kotlin-feed-url}") talkingKotlinFeedUrl: String,
        @Value("\${ks.kotlin-weekly-feed-url}") kotlinWeeklyFeedUrl: String,
    ): FeedServiceConfig {
        return FeedServiceConfig(
            kotlinBlogFeedUrl = kotlinBlogFeedUrl,
            kotlinYouTubeFeedUrl = kotlinYouTubeFeedUrl,
            talkingKotlinFeedUrl = talkingKotlinFeedUrl,
            kotlinWeeklyFeedUrl = kotlinWeeklyFeedUrl,
        )
    }

    @Bean
    fun feedStore(
        firestore: Firestore,
    ): FeedStore {
        return FirestoreFeedStore(firestore = firestore)
    }

    @Bean
    fun kotlinBlogContentStore(
        firestore: Firestore,
    ): KotlinBlogContentStore {
        return FirestoreKotlinBlogContentStore(firestore = firestore)
    }

    @Bean
    fun kotlinBlogTldrService(
        kotlinBlogContentStore: KotlinBlogContentStore,
        tldrGenerator: TldrGenerator,
    ): KotlinBlogTldrService {
        return RealKotlinBlogTldrService(
            kotlinBlogContentStore = kotlinBlogContentStore,
            tldrGenerator = tldrGenerator,
        )
    }

    @Bean
    fun kotlinWeeklyIssueService(
        engine: HttpClientEngine
    ): KotlinWeeklyIssueService {
        return RealKotlinWeeklyIssueService(
            engine = engine,
        )
    }

    @Bean
    fun tldrGenerator(
        cloudflareAiClient: CloudflareAiClient,
    ): TldrGenerator {
        return TldrGenerator(cloudflareAiClient = cloudflareAiClient)
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
