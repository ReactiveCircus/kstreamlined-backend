package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogEntry

fun KotlinBlogItem.toKotlinBlogEntry(): KotlinBlogEntry {
    return KotlinBlogEntry(
        id = guid,
        title = title,
        publishDate = pubDate,
        contentUrl = link,
        featuredImageUrl = featuredImage,
        description = description,
    )
}
