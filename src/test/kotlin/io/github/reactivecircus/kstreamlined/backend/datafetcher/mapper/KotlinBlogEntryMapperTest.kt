package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.kotlinblog.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogEntry
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinBlogEntryMapperTest {

    @Test
    fun `toKotlinBlogEntry() converts KotlinBlogItem to KotlinBlogEntry`() {
        val expected = KotlinBlogEntry(
            id = "id",
            title = "Blog title",
            publishDate = "publish date",
            contentUrl = "url",
            featuredImageUrl = "image-url",
            description = "description",
        )
        val actual = KotlinBlogItem(
            title = "Blog title",
            link = "url",
            creator = "creator",
            pubDate = "publish date",
            featuredImage = "image-url",
            categories = emptyList(),
            guid = "id",
            description = "description",
            encoded = "encoded",
            languages = emptyList(),
        ).toKotlinBlogEntry()

        assertEquals(expected, actual)
    }
}
