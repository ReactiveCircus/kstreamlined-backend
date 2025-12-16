package io.github.reactivecircus.kstreamlined.backend

import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.graphql.dgs.apq.AutomatedPersistedQueryCaffeineCache
import com.netflix.graphql.dgs.apq.DgsAPQSupportProperties
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class APQConfiguration {
    @Bean
    fun preparsedDocumentProvider(): PreparsedDocumentProvider {
        val spec = DgsAPQSupportProperties.DgsAPQDefaultCaffeineCacheProperties().caffeineSpec
        return ApolloPersistedQuerySupport(
            AutomatedPersistedQueryCaffeineCache(Caffeine.from(spec).build()),
        )
    }
}
