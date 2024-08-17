package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.datasource.FakeFeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeKotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.NoOpRedisClient
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKSConfiguration {

    @Bean
    fun feedDataSource(): FeedDataSource {
        return FakeFeedDataSource
    }

    @Bean
    fun kotlinWeeklyIssueDataSource(): KotlinWeeklyIssueDataSource {
        return FakeKotlinWeeklyIssueDataSource
    }

    @Bean
    fun redisClient(): RedisClient {
        return NoOpRedisClient
    }
}
