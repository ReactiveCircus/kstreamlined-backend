package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class TalkingKotlinEntryMapperTest {

    @Test
    fun `toTalkingKotlinEntry() converts TalkingKotlinItem to TalkingKotlin`() {
        val expected = TalkingKotlin(
            id = "id",
            title = "Podcast title",
            publishTime = Instant.parse("2022-11-22T16:30:09Z"),
            contentUrl = "url",
            audioUrl = "audio-url",
            thumbnailUrl = "image-url",
            summary = "summary",
            duration = "43min.",
        )
        val actual = TalkingKotlinItem(
            guid = "id",
            title = "Podcast title",
            pubDate = "Tue, 22 Nov 2022 16:30:09 +0000",
            link = "url",
            duration = "00:43:14",
            summary = "summary",
            enclosure = TalkingKotlinItem.Enclosure(url = "audio-url"),
            image = TalkingKotlinItem.Image(href = "image-url"),
        ).toTalkingKotlinEntry()

        assertEquals(expected, actual)
    }
}
