package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrSummary
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr

fun KotlinBlogTldrSummary.toKotlinBlogTldr(id: String): KotlinBlogTldr {
    return KotlinBlogTldr(
        id = id,
        content = content,
        model = model,
        generatedAt = generatedAt,
    )
}
