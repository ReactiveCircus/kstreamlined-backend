package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun KotlinYouTubeItem.toKotlinYouTubeEntry(): KotlinYouTube {
    return KotlinYouTube(
        id = id,
        title = title,
        publishTimestamp = LocalDateTime
            .parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toEpochSecond(ZoneOffset.UTC).toString(),
        contentUrl = link.href,
        thumbnailUrl = mediaGroup.thumbnail.url,
        description = mediaGroup.description,
    )
}
