package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTubeEntry

fun KotlinYouTubeItem.toKotlinYouTubeEntry(): KotlinYouTubeEntry {
    return KotlinYouTubeEntry(
        id = id,
        title = title,
        publishDate = published,
        contentUrl = link.href,
        thumbnailUrl = mediaGroup.thumbnail.url,
        description = mediaGroup.description,
    )
}
