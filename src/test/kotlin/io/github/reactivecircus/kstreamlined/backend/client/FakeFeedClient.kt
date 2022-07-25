package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem

object FakeFeedClient : FeedClient {

    var nextKotlinBlogFeedResponse: () -> List<KotlinBlogItem> = {
        TODO()
    }

    var nextKotlinYouTubeFeedResponse: () -> List<KotlinYouTubeItem> = {
        TODO()
    }

    var nextTalkingKotlinFeedResponse: () -> List<TalkingKotlinItem> = {
        TODO()
    }

    var nextKotlinWeeklyFeedResponse: () -> List<KotlinWeeklyItem> = {
        TODO()
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return nextKotlinBlogFeedResponse()
    }

    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> {
        return nextKotlinYouTubeFeedResponse()
    }

    override suspend fun loadTalkingKotlinFeed(): List<TalkingKotlinItem> {
        return nextTalkingKotlinFeedResponse()
    }

    override suspend fun loadKotlinWeeklyFeed(): List<KotlinWeeklyItem> {
        return nextKotlinWeeklyFeedResponse()
    }
}
