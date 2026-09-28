package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun KotlinYouTubeItem.toKotlinYouTubeEntry(): KotlinYouTube {
    return KotlinYouTube(
        id = id,
        title = title,
        publishTime = ZonedDateTime
            .parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toInstant(),
        contentUrl = link.href,
        thumbnailUrl = mediaGroup.thumbnail.url,
        description = mediaGroup.description,
    )
}
