package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin

fun TalkingKotlinItem.toTalkingKotlinEntry(logoUrl: String): TalkingKotlin {
    return TalkingKotlin(
        id = id,
        title = title,
        publishDate = published,
        contentUrl = link.href,
        podcastLogoUrl = logoUrl,
        tags = categories.map { it.term },
    )
}

const val TALKING_KOTLIN_LOGO_URL = "https://talkingkotlin.com/images/kotlin_talking_logo.png"
