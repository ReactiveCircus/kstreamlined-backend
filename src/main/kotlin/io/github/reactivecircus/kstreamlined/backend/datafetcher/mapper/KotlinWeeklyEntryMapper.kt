package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly

fun KotlinWeeklyItem.toKotlinWeeklyEntry(logoUrl: String): KotlinWeekly {
    return KotlinWeekly(
        id = guid,
        title = title.trim().removeSuffix(" -"),
        publishDate = pubDate,
        contentUrl = CONTENT_URL_REGEX.find(description)?.value.orEmpty(),
        newsletterLogoUrl = logoUrl,
    )
}

const val KOTLIN_WEEKLY_LOGO_URL = "https://pbs.twimg.com/profile_images/883969154667204608/26qTz9AE_400x400.jpg"

private val CONTENT_URL_REGEX = "https://t.co[^\"]+".toRegex()
