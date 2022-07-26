package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlog
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun KotlinBlogItem.toKotlinBlogEntry(): KotlinBlog {
    return KotlinBlog(
        id = guid,
        title = title,
        publishTimestamp = LocalDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toEpochSecond(ZoneOffset.UTC).toString(),
        contentUrl = link,
        featuredImageUrl = featuredImage,
        description = description,
    )
}
