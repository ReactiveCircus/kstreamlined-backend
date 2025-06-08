package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsTypeResolver
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toTalkingKotlinEntry
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlog
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.time.measureTimedValue

@DgsComponent
class FeedEntryDataFetcher(
    private val dataSource: FeedDataSource,
) {
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO

    @DgsQuery(field = DgsConstants.QUERY.FeedEntries)
    suspend fun feedEntries(@InputArgument filters: List<FeedSourceKey>?): List<FeedEntry> = coroutineScope {
        val (value, time) = measureTimedValue {
            FeedSourceKey.entries.filter {
                filters == null || filters.contains(it)
            }.map { source ->
                async(coroutineDispatcher) {
                    when (source) {
                        FeedSourceKey.KOTLIN_BLOG -> {
                            dataSource.loadKotlinBlogFeed().map { it.toKotlinBlogEntry() }
                        }

                        FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> {
                            dataSource.loadKotlinYouTubeFeed().map { it.toKotlinYouTubeEntry() }
                        }

                        FeedSourceKey.TALKING_KOTLIN_PODCAST -> {
                            dataSource.loadTalkingKotlinFeed().map { it.toTalkingKotlinEntry() }
                        }

                        FeedSourceKey.KOTLIN_WEEKLY -> {
                            dataSource.loadKotlinWeeklyFeed().map { it.toKotlinWeeklyEntry() }
                        }
                    }
                }
            }
                .awaitAll()
                .flatten()
                .sortedByDescending {
                    it.publishTime
                }
        }
        value
    }

    @DgsMutation(field = DgsConstants.MUTATION.SyncFeeds)
    suspend fun syncFeeds(): Boolean = coroutineScope {
        FeedSourceKey.entries.map { source ->
            async(coroutineDispatcher) {
                when (source) {
                    FeedSourceKey.KOTLIN_BLOG -> {
                        dataSource.loadKotlinBlogFeed(skipCache = true)
                    }

                    FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> {
                        dataSource.loadKotlinYouTubeFeed(skipCache = true)
                    }

                    FeedSourceKey.TALKING_KOTLIN_PODCAST -> {
                        dataSource.loadTalkingKotlinFeed(skipCache = true)
                    }

                    FeedSourceKey.KOTLIN_WEEKLY -> {
                        dataSource.loadKotlinWeeklyFeed(skipCache = true)
                    }
                }
            }
        }.awaitAll()
        true
    }

    @DgsTypeResolver(name = DgsConstants.FEEDENTRY.TYPE_NAME)
    internal fun resolveFeedEntry(feedEntry: FeedEntry): String {
        return when (feedEntry) {
            is KotlinBlog -> DgsConstants.KOTLINBLOG.TYPE_NAME
            is KotlinYouTube -> DgsConstants.KOTLINYOUTUBE.TYPE_NAME
            is TalkingKotlin -> DgsConstants.TALKINGKOTLIN.TYPE_NAME
            is KotlinWeekly -> DgsConstants.KOTLINWEEKLY.TYPE_NAME
            else -> throw IllegalStateException("Invalid type: ${feedEntry::class.simpleName}")
        }
    }
}
