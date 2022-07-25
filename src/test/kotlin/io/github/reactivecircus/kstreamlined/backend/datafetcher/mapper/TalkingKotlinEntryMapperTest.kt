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
            publishDate = "publish date",
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
            author = TalkingKotlinItem.Author(name = ""),
            published = "publish date",
            updated = "update date",
            content = TalkingKotlinItem.Content(
                type = "html",
                base = "url",
            ),
            categories = listOf(
                TalkingKotlinItem.Category("tag1"),
                TalkingKotlinItem.Category("tag2"),
            ),
            summary = TalkingKotlinItem.Summary("html"),
            thumbnail = TalkingKotlinItem.Thumbnail("thumbnail-url"),
            mediaContent = TalkingKotlinItem.MediaContent(
                medium = "image",
                url = "image-url",
            ),
        ).toTalkingKotlinEntry(logoUrl = "logo-url")

        assertEquals(expected, actual)
    }
}