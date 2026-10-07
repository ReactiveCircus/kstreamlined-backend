package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlog
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinBlogItem
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun KotlinBlogItem.toKotlinBlogEntry(): KotlinBlog {
    return KotlinBlog(
        id = guid,
        title = title,
        publishTime = ZonedDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant(),
        contentUrl = link,
        featuredImageUrl = featuredImage ?: FallbackFeatureImageUrl,
        description = description,
        hasTldrSummary = hasRawContent,
    )
}

const val FallbackFeatureImageUrl = "https://blog.jetbrains.com/wp-content/uploads/2021/07/pasted-image-0.png"
