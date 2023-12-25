package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.client.KotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import java.time.Duration

@DgsComponent
class KotlinWeeklyIssueDataFetcher(
    private val client: KotlinWeeklyIssueClient
) {
    private val cache: Cache<String, List<KotlinWeeklyIssueEntry>> = Caffeine
        .newBuilder()
        .expireAfterAccess(Duration.ofHours(1))
        .build()

    @DgsQuery(field = DgsConstants.QUERY.KotlinWeeklyIssue)
    suspend fun kotlinWeeklyIssue(@InputArgument url: String): List<KotlinWeeklyIssueEntry> {
        return cache.getIfPresent(url) ?: client.loadKotlinWeeklyIssue(url).also {
            cache.put(url, it)
        }
    }
}
