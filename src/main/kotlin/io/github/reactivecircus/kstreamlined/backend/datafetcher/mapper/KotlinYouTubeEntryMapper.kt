package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube

fun KotlinYouTubeItem.toKotlinYouTubeEntry(): KotlinYouTube {
    return KotlinYouTube(
        id = id,
        title = title,
        publishDate = published,
        contentUrl = link.href,
        thumbnailUrl = mediaGroup.thumbnail.url,
        description = mediaGroup.description,
    )
}
