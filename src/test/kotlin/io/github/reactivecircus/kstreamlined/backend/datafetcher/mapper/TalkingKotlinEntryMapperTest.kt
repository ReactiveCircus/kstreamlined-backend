package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.ContentFormat
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import java.time.Instant
import kotlin.test.Test

class TalkingKotlinEntryMapperTest {

    @Test
    fun `toTalkingKotlinEntry() converts TalkingKotlinItem to TalkingKotlin when summary format is plain text`() {
        val expected = TalkingKotlin(
            id = "id",
            title = "Podcast title",
            publishTime = Instant.parse("2022-11-22T16:30:09Z"),
            contentUrl = "url",
            audioUrl = "audio-url",
            thumbnailUrl = "image-url",
            summary = "summary line 1 line 2 line 3",
            summaryFormat = ContentFormat.TEXT,
            summaryPlainText = null,
            duration = "43min.",
        )
        val actual = TalkingKotlinItem(
            guid = "id",
            title = "Podcast title",
            pubDate = "Tue, 22 Nov 2022 16:30:09 +0000",
            link = "url",
            duration = "00:43:14",
            summary = "summary line 1\n\n line 2 \r\n   line 3",
            enclosure = TalkingKotlinItem.Enclosure(url = "audio-url"),
            image = TalkingKotlinItem.Image(href = "image-url"),
        ).toTalkingKotlinEntry()

        assert(expected == actual)
    }

    @Test
    fun `toTalkingKotlinEntry() converts TalkingKotlinItem to TalkingKotlin when summary format is HTML`() {
        val expected = TalkingKotlin(
            id = "id",
            title = "Podcast title",
            publishTime = Instant.parse("2022-11-22T16:30:09Z"),
            contentUrl = "url",
            audioUrl = "audio-url",
            thumbnailUrl = "image-url",
            summary = "<p>This is <b>HTML</b> content</p>",
            summaryFormat = ContentFormat.HTML,
            summaryPlainText = "This is HTML content",
            duration = "43min.",
        )
        val actual = TalkingKotlinItem(
            guid = "id",
            title = "Podcast title",
            pubDate = "Tue, 22 Nov 2022 16:30:09 +0000",
            link = "url",
            duration = "00:43:14",
            summary = "<p>This is <b>HTML</b> content</p>",
            enclosure = TalkingKotlinItem.Enclosure(url = "audio-url"),
            image = TalkingKotlinItem.Image(href = "image-url"),
        ).toTalkingKotlinEntry()

        assert(expected == actual)
    }
}
