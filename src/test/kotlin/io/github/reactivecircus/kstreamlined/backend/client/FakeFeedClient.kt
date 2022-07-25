package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem

object FakeFeedClient : FeedClient {

    var nextKotlinBlogFeedResponse: () -> List<KotlinBlogItem> = {
        TODO()
    }

    var nextKotlinYouTubeFeedResponse: () -> List<KotlinYouTubeItem> = {
        TODO()
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return nextKotlinBlogFeedResponse()
    }

    override suspend fun loadKotlinYouTubeFeed(): List<KotlinYouTubeItem> {
        return nextKotlinYouTubeFeedResponse()
    }
}
