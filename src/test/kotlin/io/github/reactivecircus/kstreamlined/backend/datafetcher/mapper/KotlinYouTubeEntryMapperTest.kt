package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeAuthor
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.Link
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.MediaCommunity
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.MediaGroup
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import java.time.Instant
import kotlin.test.Test

class KotlinYouTubeEntryMapperTest {

    @Test
    fun `toKotlinYouTubeEntry() converts KotlinYouTubeItem to KotlinYouTube`() {
        val expected = KotlinYouTube(
            id = "id",
            title = "Video title",
            publishTime = Instant.parse("2022-07-06T13:39:46Z"),
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
            published = "2022-07-06T13:39:46+00:00",
            updated = "2022-07-11T13:45:53+00:00",
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

        assert(expected == actual)
    }
}
