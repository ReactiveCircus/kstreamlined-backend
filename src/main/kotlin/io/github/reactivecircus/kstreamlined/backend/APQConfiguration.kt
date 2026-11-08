package io.github.reactivecircus.kstreamlined.backend

import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.graphql.dgs.apq.AutomatedPersistedQueryCaffeineCache
import graphql.execution.preparsed.PreparsedDocumentProvider
import graphql.execution.preparsed.persisted.ApolloPersistedQuerySupport
import io.github.reactivecircus.kstreamlined.backend.aot.CaffeineRuntimeHints
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportRuntimeHints

@Configuration
@ImportRuntimeHints(CaffeineRuntimeHints::class)
class APQConfiguration {
    @Bean
    fun preparsedDocumentProvider(): PreparsedDocumentProvider {
        return ApolloPersistedQuerySupport(
            AutomatedPersistedQueryCaffeineCache(
                cache = Caffeine.newBuilder().maximumSize(MaxPersistedQueries).build(),
            ),
        )
    }
}

private const val MaxPersistedQueries = 100L
