package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsTypeResolver
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlinEntry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@DgsComponent
class FeedEntryDataFetcher(
    private val feedClient: FeedClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    @DgsQuery(field = DgsConstants.QUERY.FeedEntries)
    suspend fun feedEntries(@InputArgument filters: List<FeedSourceKey>?): List<FeedEntry> = coroutineScope {
        FeedSourceKey.values().filter {
            filters == null || filters.contains(it)
        }.map { source ->
            async(coroutineDispatcher) {
                when (source) {
                    FeedSourceKey.KOTLIN_BLOG -> {
                        println("loading kotlin blog")
                        feedClient.loadKotlinBlogFeed().map { it.toKotlinBlogEntry() }
                    }
                    FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> {
                        println("loading kotlin youtube")
                        feedClient.loadKotlinYouTubeFeed().map { it.toKotlinYouTubeEntry() }
                    }
                    FeedSourceKey.TALKING_KOTLIN_PODCAST -> TODO()
                    FeedSourceKey.KOTLIN_WEEKLY -> TODO()
                }
            }
        }
            .awaitAll()
            .flatten()
            .sortedBy {
                // TODO sort
                it.publishDate
            }
    }

    @DgsTypeResolver(name = DgsConstants.FEEDENTRY.TYPE_NAME)
    internal fun resolveFeedEntry(feedEntry: FeedEntry): String {
        return when (feedEntry) {
            is KotlinBlogEntry -> DgsConstants.KOTLINBLOG.TYPE_NAME
            is KotlinYouTubeEntry -> DgsConstants.KOTLINYOUTUBE.TYPE_NAME
            is TalkingKotlinEntry -> DgsConstants.TALKINGKOTLIN.TYPE_NAME
            is KotlinWeeklyEntry -> DgsConstants.KOTLINWEEKLY.TYPE_NAME
            else -> throw IllegalStateException("Invalid type: ${feedEntry::class.simpleName}")
        }
    }
}
