package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun KotlinWeeklyItem.toKotlinWeeklyEntry(): KotlinWeekly {
    val issueNumber = linkIssueNumberRegex.find(link)?.groupValues?.get(1)?.toInt()
        ?: error("Issue number not found in link: $link")
    return KotlinWeekly(
        id = guid,
        title = title,
        publishTime = ZonedDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant(),
        contentUrl = link,
        issueNumber = issueNumber,
    )
}

private val linkIssueNumberRegex = Regex("kotlin-weekly-(\\d+)", RegexOption.IGNORE_CASE)
