package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlog
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinBlogEntryMapperTest {

    @Test
    fun `toKotlinBlogEntry() converts KotlinBlogItem to KotlinBlog`() {
        val expected = KotlinBlog(
            id = "id",
            title = "Blog title",
            publishTimestamp = "1657882575",
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
}
