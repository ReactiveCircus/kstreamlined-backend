package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinYouTube
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeContentDetails
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeSnippet
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeStatus
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeThumbnail
import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinYouTubeThumbnails
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinYouTubeEntryMapperTest {
    private val item = KotlinYouTubeItem(
        snippet = KotlinYouTubeSnippet(
            publishedAt = "2022-07-01T10:00:00Z",
            title = "Video title",
            description = "description",
            thumbnails = KotlinYouTubeThumbnails(
                high = KotlinYouTubeThumbnail(url = "image-url"),
            ),
        ),
        contentDetails = KotlinYouTubeContentDetails(
            videoId = "videoId",
            videoPublishedAt = "2022-07-06T13:39:46Z",
        ),
        status = KotlinYouTubeStatus(privacyStatus = "public"),
    )

    @Test
    fun `toKotlinYouTubeEntry() converts KotlinYouTubeItem to KotlinYouTube`() {
        val expected = KotlinYouTube(
            id = "yt:video:videoId",
            title = "Video title",
            publishTime = Instant.parse("2022-07-06T13:39:46Z"),
            contentUrl = "https://www.youtube.com/watch?v=videoId",
            thumbnailUrl = "image-url",
            description = "description",
        )

        assertEquals(expected, item.toKotlinYouTubeEntry())
    }

    @Test
    fun `toKotlinYouTubeEntry() falls back to snippet publish time when video publish time is missing`() {
        val actual = item.copy(
            contentDetails = item.contentDetails.copy(videoPublishedAt = null),
        ).toKotlinYouTubeEntry()

        assertEquals(Instant.parse("2022-07-01T10:00:00Z"), actual.publishTime)
    }

    @Test
    fun `toKotlinYouTubeEntry() falls back to default thumbnail url when high thumbnail is missing`() {
        val actual = item.copy(
            snippet = item.snippet.copy(thumbnails = KotlinYouTubeThumbnails()),
        ).toKotlinYouTubeEntry()

        assertEquals("https://i.ytimg.com/vi/videoId/hqdefault.jpg", actual.thumbnailUrl)
    }
}
