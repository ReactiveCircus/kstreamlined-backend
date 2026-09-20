package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem

class FakeFeedPersister : FeedPersister {
    private val kotlinBlogItems = mutableMapOf<String, KotlinBlogItem>()
    private val kotlinYouTubeItems = mutableMapOf<String, KotlinYouTubeItem>()
    private val talkingKotlinItems = mutableMapOf<String, TalkingKotlinItem>()
    private val kotlinWeeklyItems = mutableMapOf<String, KotlinWeeklyItem>()

    override suspend fun loadKotlinBlogItems(): List<KotlinBlogItem>? {
        return kotlinBlogItems.values.toList().ifEmpty { null }
    }

    override suspend fun saveKotlinBlogItems(items: List<KotlinBlogItem>) {
        items.forEach { kotlinBlogItems[it.guid] = it }
    }

    override suspend fun loadKotlinYouTubeItems(): List<KotlinYouTubeItem>? {
        return kotlinYouTubeItems.values.toList().ifEmpty { null }
    }

    override suspend fun saveKotlinYouTubeItems(items: List<KotlinYouTubeItem>) {
        items.forEach { kotlinYouTubeItems[it.id] = it }
    }

    override suspend fun loadTalkingKotlinItems(): List<TalkingKotlinItem>? {
        return talkingKotlinItems.values.toList().ifEmpty { null }
    }

    override suspend fun saveTalkingKotlinItems(items: List<TalkingKotlinItem>) {
        items.forEach { talkingKotlinItems[it.guid] = it }
    }

    override suspend fun loadKotlinWeeklyItems(): List<KotlinWeeklyItem>? {
        return kotlinWeeklyItems.values.toList().ifEmpty { null }
    }

    override suspend fun saveKotlinWeeklyItems(items: List<KotlinWeeklyItem>) {
        items.forEach { kotlinWeeklyItems[it.guid] = it }
    }
}
