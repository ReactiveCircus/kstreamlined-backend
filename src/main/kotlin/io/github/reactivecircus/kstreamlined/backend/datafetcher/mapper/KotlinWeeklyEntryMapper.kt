package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun KotlinWeeklyItem.toKotlinWeeklyEntry(logoUrl: String): KotlinWeekly {
    return KotlinWeekly(
        id = guid,
        title = title,
        publishTimestamp = LocalDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toEpochSecond(ZoneOffset.UTC).toString(),
        contentUrl = link,
        newsletterLogoUrl = logoUrl,
    )
}

const val KOTLIN_WEEKLY_LOGO_URL = "https://pbs.twimg.com/profile_images/883969154667204608/26qTz9AE_400x400.jpg"
