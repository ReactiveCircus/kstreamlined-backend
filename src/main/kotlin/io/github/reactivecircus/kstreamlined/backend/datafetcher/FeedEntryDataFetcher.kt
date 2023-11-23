package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsTypeResolver
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.client.CacheContext
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toTalkingKotlinEntry
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
import java.time.Duration

@DgsComponent
class FeedEntryDataFetcher(
    private val feedClient: FeedClient,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val kotlinBlogCacheContext = object : CacheContext<KotlinBlogItem> {
        override val cache: Cache<Unit, List<KotlinBlogItem>> = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build()
    }

    private val kotlinYouTubeCacheContext = object : CacheContext<KotlinYouTubeItem> {
        override val cache: Cache<Unit, List<KotlinYouTubeItem>> = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build()
    }

    private val talkingKotlinCacheContext = object : CacheContext<TalkingKotlinItem> {
        override val cache: Cache<Unit, List<TalkingKotlinItem>> = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build()
    }

    private val kotlinWeeklyCacheContext = object : CacheContext<KotlinWeeklyItem> {
        override val cache: Cache<Unit, List<KotlinWeeklyItem>> = Caffeine
            .newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .build()
    }

    @DgsQuery(field = DgsConstants.QUERY.FeedEntries)
    suspend fun feedEntries(@InputArgument filters: List<FeedSourceKey>?): List<FeedEntry> = coroutineScope {
        FeedSourceKey.entries.filter {
            filters == null || filters.contains(it)
        }.map { source ->
            async(coroutineDispatcher) {
                when (source) {
                    FeedSourceKey.KOTLIN_BLOG -> with(kotlinBlogCacheContext) {
                        feedClient.loadKotlinBlogFeed().map { it.toKotlinBlogEntry() }
                    }

                    FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> with(kotlinYouTubeCacheContext) {
                        feedClient.loadKotlinYouTubeFeed().map { it.toKotlinYouTubeEntry() }
                    }

                    FeedSourceKey.TALKING_KOTLIN_PODCAST -> with(talkingKotlinCacheContext) {
                        feedClient.loadTalkingKotlinFeed().map { it.toTalkingKotlinEntry() }
                    }

                    FeedSourceKey.KOTLIN_WEEKLY -> with(kotlinWeeklyCacheContext) {
                        feedClient.loadKotlinWeeklyFeed().map { it.toKotlinWeeklyEntry() }
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
