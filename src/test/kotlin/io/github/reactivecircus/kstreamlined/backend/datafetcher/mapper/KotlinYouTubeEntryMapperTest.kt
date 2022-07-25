package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeAuthor
import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaCommunity
import io.github.reactivecircus.kstreamlined.backend.client.dto.MediaGroup
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinYouTubeEntryMapperTest {

    @Test
    fun `toKotlinYouTubeEntry() converts KotlinYouTubeItem to KotlinYouTube`() {
        val expected = KotlinYouTube(
            id = "id",
            title = "Video title",
            publishDate = "publish date",
            contentUrl = "url",
            thumbnailUrl = "image-url",
            description = "description",
        )
        val actual = KotlinYouTubeItem(
            id = "id",
            videoId = "videoId",
            channelId = "channelId",
            title = "Video title",
            link = Link(href = "url", rel = "alternate"),
            author = KotlinYouTubeAuthor(name = "author", uri = "uri"),
            published = "publish date",
            updated = "update date",
            mediaGroup = MediaGroup(
                title = "title",
                content = MediaGroup.Content(
                    url = "url",
                    type = "type",
                    width = "width",
                    height = "height",
                ),
                thumbnail = MediaGroup.Thumbnail(
                    url = "image-url",
                    width = "640",
                    height = "390",
                ),
                description = "description",
                community = MediaCommunity(
                    starRating = MediaCommunity.StarRating(
                        count = "100",
                        average = "5.0",
                        min = "1",
                        max = "5",
                    ),
                    statistics = MediaCommunity.Statistics(views = "1000"),
                ),
            )
        ).toKotlinYouTubeEntry()

        assertEquals(expected, actual)
    }
}
