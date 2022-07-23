package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsQuery
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSource
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey

@DgsComponent
class FeedSourceDataFetcher {

    @DgsQuery(field = DgsConstants.QUERY.FeedSources)
    suspend fun feedSources(): List<FeedSource> {
        return FeedSourceKey.values().map {
            val title = when (it) {
                FeedSourceKey.KOTLIN_BLOG -> FeedSourceTitle.KotlinBlog
                FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> FeedSourceTitle.KotlinYouTube
                FeedSourceKey.TALKING_KOTLIN_PODCAST -> FeedSourceTitle.TalkingKotlin
                FeedSourceKey.KOTLIN_WEEKLY -> FeedSourceTitle.KotlinWeekly
            }
            val description = when (it) {
                FeedSourceKey.KOTLIN_BLOG -> FeedSourceDescription.KotlinBlog
                FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL -> FeedSourceDescription.KotlinYouTube
                FeedSourceKey.TALKING_KOTLIN_PODCAST -> FeedSourceDescription.TalkingKotlin
                FeedSourceKey.KOTLIN_WEEKLY -> FeedSourceDescription.KotlinWeekly
            }
            FeedSource(key = it, title = title, description = description)
        }
    }
}

internal object FeedSourceTitle {
    const val KotlinBlog = "Kotlin Blog"
    const val KotlinYouTube = "Kotlin YouTube"
    const val TalkingKotlin = "Talking Kotlin"
    const val KotlinWeekly = "Kotlin Weekly"
}

internal object FeedSourceDescription {
    const val KotlinBlog = "Latest news from the official Kotlin Blog"
    const val KotlinYouTube = "The official YouTube channel of the Kotlin programming language"
    const val TalkingKotlin = "Technical show discussing everything Kotlin, hosted by Hadi and Sebastian"
    const val KotlinWeekly = "Weekly community Kotlin newsletter, hosted by Enrique"
}
