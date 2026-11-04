package io.github.reactivecircus.kstreamlined.backend.service.dto

import io.github.reactivecircus.kstreamlined.backend.store.FirestoreModel
import kotlinx.serialization.Serializable

@Serializable
data class KotlinYouTubePlaylistItems(
    val items: List<KotlinYouTubeItem>,
)

@FirestoreModel
@Serializable
data class KotlinYouTubeItem(
    val snippet: KotlinYouTubeSnippet,
    val contentDetails: KotlinYouTubeContentDetails,
    val status: KotlinYouTubeStatus,
) {
    val id: String = "yt:video:${contentDetails.videoId}"
}

@FirestoreModel
@Serializable
data class KotlinYouTubeSnippet(
    val publishedAt: String,
    val title: String,
    val description: String,
    val thumbnails: KotlinYouTubeThumbnails = KotlinYouTubeThumbnails(),
)

@FirestoreModel
@Serializable
data class KotlinYouTubeThumbnails(
    val high: KotlinYouTubeThumbnail? = null,
)

@FirestoreModel
@Serializable
data class KotlinYouTubeThumbnail(
    val url: String,
)

@FirestoreModel
@Serializable
data class KotlinYouTubeContentDetails(
    val videoId: String,
    val videoPublishedAt: String? = null,
)

@FirestoreModel
@Serializable
data class KotlinYouTubeStatus(
    val privacyStatus: String,
)
