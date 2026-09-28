package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.github.reactivecircus.kstreamlined.backend.service.FakeFeedService
import io.github.reactivecircus.kstreamlined.backend.service.FakeKotlinBlogTldrService
import io.github.reactivecircus.kstreamlined.backend.service.FakeKotlinWeeklyIssueService
import io.github.reactivecircus.kstreamlined.backend.service.FeedService
import io.github.reactivecircus.kstreamlined.backend.service.KotlinBlogTldrService
import io.github.reactivecircus.kstreamlined.backend.service.KotlinWeeklyIssueService
import io.github.reactivecircus.kstreamlined.backend.service.NoOpRedisClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKSConfiguration {
    @Bean
    fun feedService(): FeedService {
        return FakeFeedService
    }

    @Bean
    fun kotlinWeeklyIssueService(): KotlinWeeklyIssueService {
        return FakeKotlinWeeklyIssueService
    }

    @Bean
    fun kotlinBlogTldrService(): KotlinBlogTldrService {
        return FakeKotlinBlogTldrService()
    }

    @Bean
    fun redisClient(): RedisClient {
        return NoOpRedisClient
    }
}
