package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import io.github.reactivecircus.kstreamlined.backend.service.KotlinWeeklyIssueService
import java.time.Duration

@DgsComponent
class KotlinWeeklyIssueDataFetcher(
    private val service: KotlinWeeklyIssueService
) {
    private val cache: Cache<String, List<KotlinWeeklyIssueEntry>> = Caffeine
        .newBuilder()
        .expireAfterWrite(Duration.ofHours(1))
        .build()

    @DgsQuery(field = DgsConstants.QUERY.KotlinWeeklyIssue)
    suspend fun kotlinWeeklyIssue(@InputArgument url: String): List<KotlinWeeklyIssueEntry> {
        return cache.getIfPresent(url) ?: service.loadKotlinWeeklyIssue(url).also {
            cache.put(url, it)
        }
    }
}
