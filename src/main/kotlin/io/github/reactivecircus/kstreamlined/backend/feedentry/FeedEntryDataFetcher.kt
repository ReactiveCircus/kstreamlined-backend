package io.github.reactivecircus.kstreamlined.backend.feedentry

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.DgsTypeResolver
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlinEntry

@DgsComponent
class FeedEntryDataFetcher {

    @DgsQuery(field = DgsConstants.QUERY.FeedEntries)
    suspend fun feedEntries(): List<FeedEntry> {
        // TODO
        return listOf(
            KotlinBlogEntry(
                id = "1",
                title = "Blog title",
                publishDate = "date",
                contentUrl = "url",
                featuredImageUrl = "image-url",
                description = "description",
            )
        )
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
