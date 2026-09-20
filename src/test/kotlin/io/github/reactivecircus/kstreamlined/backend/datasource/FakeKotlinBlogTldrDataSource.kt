package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import java.time.Instant

class FakeKotlinBlogTldrDataSource : KotlinBlogTldrDataSource {
    var nextKotlinBlogTldrResponse: suspend (String) -> KotlinBlogContent.Tldr? = { null }

    var nextGenerateKotlinBlogTldrResponse: suspend (String, Boolean) -> KotlinBlogContent.Tldr = { _, _ ->
        error("No Kotlin Blog TLDR generation response configured.")
    }

    var nextBackfillKotlinBlogTldrsResponse: suspend () -> KotlinBlogTldrBackfillResult = {
        error("No Kotlin Blog TLDR backfill response configured.")
    }

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogContent.Tldr? {
        return nextKotlinBlogTldrResponse(id)
    }

    override suspend fun createKotlinBlogTldr(id: String, persist: Boolean): KotlinBlogContent.Tldr {
        return nextGenerateKotlinBlogTldrResponse(id, persist)
    }

    override suspend fun backfillKotlinBlogTldrs(): KotlinBlogTldrBackfillResult {
        return nextBackfillKotlinBlogTldrsResponse()
    }
}

val DummyKotlinBlogTldr = KotlinBlogContent.Tldr(
    output = "**Structured concurrency** keeps related work together.\n\n" +
        "```kotlin\ncoroutineScope { launch { work() } }\n```\n\n" +
        "[Docs](https://kotlinlang.org/docs/coroutines-basics.html)",
    model = "gpt-oss-120b",
    generatedAt = Instant.parse("2026-09-14T12:00:00.123456Z"),
    promptTokens = 1_000,
    completionTokens = 200,
    totalTokens = 1_200,
    neurons = 75.5,
    generationDurationMs = 10_000,
)
