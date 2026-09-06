package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyTalkingKotlinItem
import kotlin.test.Test
import kotlin.test.assertEquals

class FirestoreDocumentIdTest {
    @Test
    fun `String can be converted to expected firebaseDocumentId`() {
        assertEquals("12345", "https://blog.jetbrains.com?id=12345".firestoreDocumentId)
    }

    @Test
    fun `KotlinBlogItem has expected firebaseDocumentId`() {
        val kotlinBlogItem = DummyKotlinBlogItem.copy(guid = "https://blog.jetbrains.com?id=12345")

        assertEquals("12345", kotlinBlogItem.firestoreDocumentId)
    }

    @Test
    fun `KotlinYouTubeItem has expected firebaseDocumentId`() {
        val kotlinYouTubeItem = DummyKotlinYouTubeItem.copy(id = "yt:video:abcde12345")
        assertEquals("yt:video:abcde12345", kotlinYouTubeItem.firestoreDocumentId)
    }

    @Test
    fun `TalkingKotlinItem has expected firebaseDocumentId`() {
        val talkingKotlinItem = DummyTalkingKotlinItem.copy(guid = "tag:soundcloud,2010:tracks/12345")
        assertEquals("tag:soundcloud,2010:tracks-12345", talkingKotlinItem.firestoreDocumentId)
    }

    @Test
    fun `KotlinWeeklyItem has expected firebaseDocumentId`() {
        val kotlinWeeklyItem = DummyKotlinWeeklyItem.copy(guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-123")
        assertEquals("kotlin-weekly-123", kotlinWeeklyItem.firestoreDocumentId)
    }
}
