package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinBlogTldrMapperTest {
    @Test
    fun `toKotlinBlogTldr() converts KotlinBlogContent#Tldr to KotlinBlogTldr`() {
        val generatedAt = Instant.parse("2026-09-14T12:00:00Z")
        val expected = KotlinBlogTldr(
            id = "12345",
            content = "Generated TLDR.",
            model = "gpt-oss-120b",
            generatedAt = generatedAt,
        )
        val actual = KotlinBlogContent.Tldr(
            output = "Generated TLDR.",
            model = "gpt-oss-120b",
            generatedAt = generatedAt,
            promptTokens = 100,
            completionTokens = 50,
            totalTokens = 150,
            neurons = 0.75,
            generationDurationMs = 2000L,
        ).toKotlinBlogTldr("12345")

        assertEquals(expected, actual)
    }
}
