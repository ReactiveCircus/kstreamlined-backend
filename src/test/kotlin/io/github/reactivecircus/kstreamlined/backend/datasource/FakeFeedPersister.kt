package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem

class FakeFeedPersister : FeedPersister {

    private val kotlinBlogItems = mutableMapOf<String, KotlinBlogItem>()
    private val kotlinYouTubeItems = mutableMapOf<String, KotlinYouTubeItem>()
    private val talkingKotlinItems = mutableMapOf<String, TalkingKotlinItem>()
    private val kotlinWeeklyItems = mutableMapOf<String, KotlinWeeklyItem>()

    override fun loadKotlinBlogItems(): List<KotlinBlogItem>? {
        return kotlinBlogItems.values.toList().ifEmpty { null }
    }

    override fun saveKotlinBlogItems(items: List<KotlinBlogItem>) {
        items.forEach { kotlinBlogItems[it.guid] = it }
    }

    override fun loadKotlinYouTubeItems(): List<KotlinYouTubeItem>? {
        return kotlinYouTubeItems.values.toList().ifEmpty { null }
    }

    override fun saveKotlinYouTubeItems(items: List<KotlinYouTubeItem>) {
        items.forEach { kotlinYouTubeItems[it.id] = it }
    }

    override fun loadTalkingKotlinItems(): List<TalkingKotlinItem>? {
        return talkingKotlinItems.values.toList().ifEmpty { null }
    }

    override fun saveTalkingKotlinItems(items: List<TalkingKotlinItem>) {
        items.forEach { talkingKotlinItems[it.guid] = it }
    }

    override fun loadKotlinWeeklyItems(): List<KotlinWeeklyItem>? {
        return kotlinWeeklyItems.values.toList().ifEmpty { null }
    }

    override fun saveKotlinWeeklyItems(items: List<KotlinWeeklyItem>) {
        items.forEach { kotlinWeeklyItems[it.guid] = it }
    }
}
