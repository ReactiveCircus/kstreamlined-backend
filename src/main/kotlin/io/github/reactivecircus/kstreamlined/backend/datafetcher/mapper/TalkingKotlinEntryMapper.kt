package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.ContentFormat
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import java.time.Duration
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun TalkingKotlinItem.toTalkingKotlinEntry(): TalkingKotlin {
    val rawSummary = summary.sanitize()
    val parsedDoc = tryParseHtml(rawSummary)
    val summaryFormat = if (parsedDoc != null) ContentFormat.HTML else ContentFormat.TEXT
    val summaryPlainText = parsedDoc?.text()

    return TalkingKotlin(
        id = guid,
        title = title,
        publishTime = ZonedDateTime
            .parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
            .toInstant(),
        contentUrl = link,
        thumbnailUrl = image.href,
        audioUrl = enclosure.url,
        summary = rawSummary,
        summaryFormat = summaryFormat,
        summaryPlainText = summaryPlainText,
        duration = duration.toFormattedDuration(),
    )
}

private fun String.sanitize(): String {
    return replace(Regex("[\n\r]+"), " ")
        .replace(Regex("\\s+"), " ")
}

private fun String.toFormattedDuration(): String {
    val parts = split(":").map { it.toInt() }
    val duration = Duration.ofHours(parts[0].toLong())
        .plusMinutes(parts[1].toLong())
        .plusSeconds(parts[2].toLong())

    val hours = duration.toHoursPart()
    val minutes = duration.toMinutesPart()

    return if (hours > 0) "${hours}h ${minutes}min." else "${minutes}min."
}
