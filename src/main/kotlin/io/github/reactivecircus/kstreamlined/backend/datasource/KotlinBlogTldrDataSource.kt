package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrSummary
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrInputExtractor
import java.time.Clock
import java.time.Instant

interface KotlinBlogTldrDataSource {
    suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary?

    suspend fun createKotlinBlogTldr(id: String, persist: Boolean): KotlinBlogTldrSummary
}

class RealKotlinBlogTldrDataSource(
    private val kotlinBlogContentPersister: KotlinBlogContentPersister,
    private val kotlinBlogTldrPersister: KotlinBlogTldrPersister,
    private val tldrGenerator: TldrGenerator,
    private val clock: Clock = Clock.systemUTC(),
) : KotlinBlogTldrDataSource {
    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
        kotlinBlogTldrPersister.loadKotlinBlogTldr(id)?.let { return it }

        return kotlinBlogContentPersister.loadKotlinBlogContent(id)?.let { article ->
            val tldr = generateSummary(article)
            kotlinBlogTldrPersister.saveKotlinBlogTldr(id, tldr)
            tldr
        }
    }

    override suspend fun createKotlinBlogTldr(id: String, persist: Boolean): KotlinBlogTldrSummary {
        val article = checkNotNull(kotlinBlogContentPersister.loadKotlinBlogContent(id)) {
            "Kotlin Blog content not found for article: $id."
        }
        val tldr = generateSummary(article)
        if (persist) {
            kotlinBlogTldrPersister.saveKotlinBlogTldr(id, tldr)
        }
        return tldr
    }

    private suspend fun generateSummary(article: KotlinBlogContent): KotlinBlogTldrSummary {
        val result = tldrGenerator.generate(
            title = article.title,
            articleText = TldrInputExtractor.extract(article.html),
        )
        return KotlinBlogTldrSummary(
            content = result.content,
            model = result.model,
            generatedAt = Instant.now(clock),
            promptTokens = result.promptTokens,
            completionTokens = result.completionTokens,
            totalTokens = result.totalTokens,
            neurons = result.neurons,
            generationDurationMs = result.requestLatencyMs,
        )
    }
}
