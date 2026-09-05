package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.cloud.firestore.DocumentReference
import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

interface FeedPersister {
    fun loadKotlinBlogItems(): List<KotlinBlogItem>?

    fun saveKotlinBlogItems(items: List<KotlinBlogItem>)

    fun loadKotlinYouTubeItems(): List<KotlinYouTubeItem>?

    fun saveKotlinYouTubeItems(items: List<KotlinYouTubeItem>)

    fun loadTalkingKotlinItems(): List<TalkingKotlinItem>?

    fun saveTalkingKotlinItems(items: List<TalkingKotlinItem>)

    fun loadKotlinWeeklyItems(): List<KotlinWeeklyItem>?

    fun saveKotlinWeeklyItems(items: List<KotlinWeeklyItem>)
}

class FirestoreFeedPersister(
    private val firestore: Firestore,
) : FeedPersister {
    override fun loadKotlinBlogItems(): List<KotlinBlogItem>? {
        return firestore.collection(FeedCollectionPath.KotlinBlog).get().get().map {
            it.toObject(KotlinBlogItem::class.java)
        }.ifEmpty { null }
    }

    override fun saveKotlinBlogItems(items: List<KotlinBlogItem>) {
        batchWrite(items) { item ->
            firestore.collection(FeedCollectionPath.KotlinBlog)
                .document(item.firestoreDocumentId)
        }
    }

    override fun loadKotlinYouTubeItems(): List<KotlinYouTubeItem>? {
        return firestore.collection(FeedCollectionPath.KotlinYouTube).get().get().map {
            it.toObject(KotlinYouTubeItem::class.java)
        }.ifEmpty { null }
    }

    override fun saveKotlinYouTubeItems(items: List<KotlinYouTubeItem>) {
        batchWrite(items) { item ->
            firestore.collection(FeedCollectionPath.KotlinYouTube)
                .document(item.firestoreDocumentId)
        }
    }

    override fun loadTalkingKotlinItems(): List<TalkingKotlinItem>? {
        return firestore.collection(FeedCollectionPath.TalkingKotlin).get().get().map {
            it.toObject(TalkingKotlinItem::class.java)
        }
            .sortedByDescending {
                ZonedDateTime.parse(it.pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            }
            .take(TalkingKotlinFeedSize)
            .ifEmpty { null }
    }

    override fun saveTalkingKotlinItems(items: List<TalkingKotlinItem>) {
        batchWrite(items) { item ->
            firestore.collection(FeedCollectionPath.TalkingKotlin)
                .document(item.firestoreDocumentId)
        }
    }

    override fun loadKotlinWeeklyItems(): List<KotlinWeeklyItem>? {
        return firestore.collection(FeedCollectionPath.KotlinWeekly).get().get().map {
            it.toObject(KotlinWeeklyItem::class.java)
        }.ifEmpty { null }
    }

    override fun saveKotlinWeeklyItems(items: List<KotlinWeeklyItem>) {
        batchWrite(items) { item ->
            firestore.collection(FeedCollectionPath.KotlinWeekly)
                .document(item.firestoreDocumentId)
        }
    }

    private inline fun <T : Any> batchWrite(items: List<T>, docRef: (T) -> DocumentReference) {
        firestore.batch().apply {
            items.forEach { item ->
                set(docRef(item), item)
            }
        }.commit().get()
    }
}

private const val TalkingKotlinFeedSize = 10

private object FeedCollectionPath {
    const val KotlinBlog = "kotlin_blog_feed"
    const val KotlinYouTube = "kotlin_youtube_feed"
    const val TalkingKotlin = "talking_kotlin_feed"
    const val KotlinWeekly = "kotlin_weekly_feed"
}
