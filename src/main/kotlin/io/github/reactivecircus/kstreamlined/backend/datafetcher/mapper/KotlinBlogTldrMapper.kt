package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.store.KotlinBlogContent

fun KotlinBlogContent.Tldr.toKotlinBlogTldr(id: String): KotlinBlogTldr {
    return KotlinBlogTldr(
        id = id,
        content = output,
        model = model,
        generatedAt = generatedAt,
    )
}
