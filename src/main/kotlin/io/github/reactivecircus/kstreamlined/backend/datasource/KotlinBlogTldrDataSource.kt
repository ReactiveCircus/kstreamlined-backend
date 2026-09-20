package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrInputExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant

interface KotlinBlogTldrDataSource {
    suspend fun loadKotlinBlogTldr(id: String): KotlinBlogContent.Tldr?

    suspend fun createKotlinBlogTldr(id: String, persist: Boolean): KotlinBlogContent.Tldr

    suspend fun backfillKotlinBlogTldrs(): KotlinBlogTldrBackfillResult
}

data class KotlinBlogTldrBackfillResult(
    val generatedCount: Int,
    val failedIds: List<String>,
)

class RealKotlinBlogTldrDataSource(
    private val kotlinBlogContentPersister: KotlinBlogContentPersister,
    private val tldrGenerator: TldrGenerator,
    private val clock: Clock = Clock.systemUTC(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : KotlinBlogTldrDataSource {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogContent.Tldr? {
        return kotlinBlogContentPersister.loadKotlinBlogContent(id)?.let { content ->
            content.tldr?.let { return it }
            val tldr = generateTldr(content)
            kotlinBlogContentPersister.saveKotlinBlogTldrs(mapOf(id to tldr))
            tldr
        }
    }

    override suspend fun createKotlinBlogTldr(id: String, persist: Boolean): KotlinBlogContent.Tldr {
        val content = checkNotNull(kotlinBlogContentPersister.loadKotlinBlogContent(id)) {
            "Kotlin Blog content not found for article: $id."
        }
        val tldr = generateTldr(content)
        if (persist) {
            kotlinBlogContentPersister.saveKotlinBlogTldrs(mapOf(id to tldr))
        }
        return tldr
    }

    override suspend fun backfillKotlinBlogTldrs(): KotlinBlogTldrBackfillResult = coroutineScope {
        val contents = kotlinBlogContentPersister.loadKotlinBlogContentsWithoutTldr()
        val outcomes = contents.map { content ->
            async(dispatcher) {
                runCatching {
                    val tldr = generateTldr(content)
                    BackfillOutcome.Generated(id = content.id, tldr = tldr)
                }.getOrElse { t ->
                    if (t is CancellationException) currentCoroutineContext().ensureActive()
                    logger.atError()
                        .addKeyValue("kotlinBlogId", content.id)
                        .setCause(t)
                        .log("Kotlin Blog TLDR backfill failed for article: {}", content.id)
                    BackfillOutcome.Failed(content.id)
                }
            }
        }.awaitAll()
        val generatedTldrs = outcomes.filterIsInstance<BackfillOutcome.Generated>()
        kotlinBlogContentPersister.saveKotlinBlogTldrs(
            tldrs = generatedTldrs.associate { it.id to it.tldr },
        )
        val result = KotlinBlogTldrBackfillResult(
            generatedCount = generatedTldrs.size,
            failedIds = outcomes.filterIsInstance<BackfillOutcome.Failed>().map { it.id },
        )
        result
    }

    private suspend fun generateTldr(content: KotlinBlogContent): KotlinBlogContent.Tldr {
        val result = tldrGenerator.generate(
            title = content.title,
            articleText = TldrInputExtractor.extract(content.html),
        )
        return KotlinBlogContent.Tldr(
            output = result.content,
            model = result.model,
            generatedAt = Instant.now(clock),
            promptTokens = result.promptTokens,
            completionTokens = result.completionTokens,
            totalTokens = result.totalTokens,
            neurons = result.neurons,
            generationDurationMs = result.requestLatencyMs,
        )
    }

    private sealed interface BackfillOutcome {
        class Generated(val id: String, val tldr: KotlinBlogContent.Tldr) : BackfillOutcome
        class Failed(val id: String) : BackfillOutcome
    }
}
