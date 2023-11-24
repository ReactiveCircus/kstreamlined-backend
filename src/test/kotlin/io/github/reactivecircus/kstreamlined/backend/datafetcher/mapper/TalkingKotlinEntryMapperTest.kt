package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
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
            publishTime = Instant.parse("2022-06-27T22:00:00Z"),
            contentUrl = "url",
            podcastLogoUrl = TalkingKotlinLogoUrl,
            tags = listOf(
                "tag1",
                "tag2",
            ),
        )
        val actual = TalkingKotlinItem(
            id = "id",
            title = "Podcast title",
            link = Link(
                href = "url",
                rel = "alternate",
                type = "text/html",
                title = "Podcast title",
            ),
            published = "2022-06-28T00:00:00+02:00",
            categories = listOf(
                TalkingKotlinItem.Category("tag1"),
                TalkingKotlinItem.Category("tag2"),
            ),
        ).toTalkingKotlinEntry()

        assertEquals(expected, actual)
    }
}
