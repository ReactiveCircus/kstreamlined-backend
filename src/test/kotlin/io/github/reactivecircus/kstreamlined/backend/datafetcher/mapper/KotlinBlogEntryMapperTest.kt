package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlog
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals

class KotlinBlogEntryMapperTest {

    @Test
    fun `toKotlinBlogEntry() converts KotlinBlogItem to KotlinBlog`() {
        val expected = KotlinBlog(
            id = "id",
            title = "Blog title",
            publishTime = Instant.parse("2022-07-15T10:56:15Z"),
            contentUrl = "url",
            featuredImageUrl = "image-url",
            description = "description",
        )
        val actual = KotlinBlogItem(
            title = "Blog title",
            link = "url",
            pubDate = "Fri, 15 Jul 2022 10:56:15 +0000",
            featuredImage = "image-url",
            guid = "id",
            description = "description",
        ).toKotlinBlogEntry()

        assertEquals(expected, actual)
    }

    @Test
    fun `toKotlinBlogEntry() converts KotlinBlogItem to KotlinBlog with fallback featuredImageUrl when missing`() {
        val expected = KotlinBlog(
            id = "id",
            title = "Blog title",
            publishTime = Instant.parse("2022-07-15T10:56:15Z"),
            contentUrl = "url",
            featuredImageUrl = FallbackFeatureImageUrl,
            description = "description",
        )
        val actual = KotlinBlogItem(
            title = "Blog title",
            link = "url",
            pubDate = "Fri, 15 Jul 2022 10:56:15 +0000",
            featuredImage = null,
            guid = "id",
            description = "description",
        ).toKotlinBlogEntry()

        assertEquals(expected, actual)
    }
}
