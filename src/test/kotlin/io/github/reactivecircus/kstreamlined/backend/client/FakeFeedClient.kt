package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem

object FakeFeedClient : FeedClient {

    var nextKotlinBlogFeedResponse: () -> List<KotlinBlogItem> = {
        TODO()
    }

    override suspend fun loadKotlinBlogFeed(): List<KotlinBlogItem> {
        return nextKotlinBlogFeedResponse()
    }
}
