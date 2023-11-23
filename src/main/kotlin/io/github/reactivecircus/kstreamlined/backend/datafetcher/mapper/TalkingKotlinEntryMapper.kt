package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun TalkingKotlinItem.toTalkingKotlinEntry(): TalkingKotlin {
    return TalkingKotlin(
        id = id,
        title = title,
        publishTime = ZonedDateTime
            .parse(published, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .format(DateTimeFormatter.ISO_INSTANT),
        contentUrl = link.href,
        podcastLogoUrl = TalkingKotlinLogoUrl,
        tags = categories.map { it.term },
    )
}

const val TalkingKotlinLogoUrl = "https://talkingkotlin.com/images/kotlin_talking_logo.png"
