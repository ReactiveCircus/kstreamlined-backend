package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.client.FakeFeedClient
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TestKSConfiguration {

    @Bean
    fun feedClient(): FeedClient {
        return FakeFeedClient
    }
}
