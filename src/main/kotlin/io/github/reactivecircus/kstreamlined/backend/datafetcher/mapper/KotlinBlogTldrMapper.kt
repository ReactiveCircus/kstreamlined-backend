package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr

fun KotlinBlogContent.Tldr.toKotlinBlogTldr(id: String): KotlinBlogTldr {
    return KotlinBlogTldr(
        id = id,
        content = output,
        model = model,
        generatedAt = generatedAt,
    )
}
