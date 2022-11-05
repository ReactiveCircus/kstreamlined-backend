package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TalkingKotlinEntryMapperTest {

    @Test
    fun `toTalkingKotlinEntry() converts TalkingKotlinItem to TalkingKotlin`() {
        val expected = TalkingKotlin(
            id = "id",
            title = "Podcast title",
            publishTimestamp = "1656374400",
            contentUrl = "url",
            podcastLogoUrl = "logo-url",
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
        ).toTalkingKotlinEntry(logoUrl = "logo-url")

        assertEquals(expected, actual)
    }
}
