package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun KotlinYouTubeItem.toKotlinYouTubeEntry(): KotlinYouTube {
    val videoId = contentDetails.videoId
    return KotlinYouTube(
        id = id,
        title = snippet.title,
        publishTime = ZonedDateTime
            .parse(contentDetails.videoPublishedAt ?: snippet.publishedAt, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toInstant(),
        contentUrl = "https://www.youtube.com/watch?v=$videoId",
        videoId = videoId,
        thumbnailUrl = snippet.thumbnails.high?.url ?: "https://i.ytimg.com/vi/$videoId/hqdefault.jpg",
        description = snippet.description,
    )
}
