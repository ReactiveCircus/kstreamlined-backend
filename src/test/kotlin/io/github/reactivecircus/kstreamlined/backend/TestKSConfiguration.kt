package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.client.FakeFeedClient
import io.github.reactivecircus.kstreamlined.backend.client.FakeKotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.client.KotlinWeeklyIssueClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKSConfiguration {

    @Bean
    fun feedClient(): FeedClient {
        return FakeFeedClient
    }

    @Bean
    fun kotlinWeeklyIssueClient(): KotlinWeeklyIssueClient {
        return FakeKotlinWeeklyIssueClient
    }
}
