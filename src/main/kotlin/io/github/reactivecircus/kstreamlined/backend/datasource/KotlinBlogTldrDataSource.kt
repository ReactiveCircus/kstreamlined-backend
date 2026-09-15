package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrPersister
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrInputExtractor
import java.time.Clock
import java.time.Instant

interface KotlinBlogTldrDataSource {
    suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr
}

class RealKotlinBlogTldrDataSource(
    private val kotlinBlogContentPersister: KotlinBlogContentPersister,
    private val kotlinBlogTldrPersister: KotlinBlogTldrPersister,
    private val tldrGenerator: TldrGenerator,
    private val clock: Clock = Clock.systemUTC(),
) : KotlinBlogTldrDataSource {
    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr {
        kotlinBlogTldrPersister.loadKotlinBlogTldr(id)?.let { return it }

        val article = kotlinBlogContentPersister.loadKotlinBlogContent(id)
            ?: throw KotlinBlogContentNotFoundException(id)
        val result = tldrGenerator.generate(
            title = article.title,
            articleText = TldrInputExtractor.extract(article.html),
        )
        val tldr = KotlinBlogTldr(
            content = result.content,
            model = result.model,
            generatedAt = Instant.now(clock),
            promptTokens = result.promptTokens,
            completionTokens = result.completionTokens,
            totalTokens = result.totalTokens,
            neurons = result.neurons,
            generationDurationMs = result.requestLatencyMs,
        )
        kotlinBlogTldrPersister.saveKotlinBlogTldr(id, tldr)
        return tldr
    }
}

class KotlinBlogContentNotFoundException(
    id: String,
) : RuntimeException("Kotlin Blog content not found for article: $id.")
