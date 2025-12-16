package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.datasource.FakeFeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeKotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.NoOpRedisClient
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestKSConfiguration {
    @Bean
    @Primary
    fun testFeedDataSource(): FeedDataSource {
        return FakeFeedDataSource
    }

    @Bean
    @Primary
    fun testKotlinWeeklyIssueDataSource(): KotlinWeeklyIssueDataSource {
        return FakeKotlinWeeklyIssueDataSource
    }

    @Bean
    @Primary
    fun testRedisClient(): RedisClient {
        return NoOpRedisClient
    }
}
