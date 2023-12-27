package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun KotlinWeeklyItem.toKotlinWeeklyEntry(): KotlinWeekly {
    return KotlinWeekly(
        id = guid,
        title = title,
        publishTime = ZonedDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant(),
        contentUrl = link,
        issueNumber = title.substringAfter("#").toInt(),
    )
}
