package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun TalkingKotlinItem.toTalkingKotlinEntry(logoUrl: String): TalkingKotlin {
    return TalkingKotlin(
        id = id,
        title = title,
        publishTimestamp = LocalDateTime
            .parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toEpochSecond(ZoneOffset.UTC).toString(),
        contentUrl = link.href,
        podcastLogoUrl = logoUrl,
        tags = categories.map { it.term },
    )
}

const val TALKING_KOTLIN_LOGO_URL = "https://talkingkotlin.com/images/kotlin_talking_logo.png"
