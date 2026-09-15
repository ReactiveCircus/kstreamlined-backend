package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrSummary
import java.time.Instant

class FakeKotlinBlogTldrDataSource : KotlinBlogTldrDataSource {
    var nextKotlinBlogTldrResponse: suspend (String) -> KotlinBlogTldrSummary? = { null }

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
        return nextKotlinBlogTldrResponse(id)
    }
}

val DummyKotlinBlogTldrSummary = KotlinBlogTldrSummary(
    content = "**Structured concurrency** keeps related work together.\n\n" +
        "```kotlin\ncoroutineScope { launch { work() } }\n```\n\n" +
        "[Docs](https://kotlinlang.org/docs/coroutines-basics.html)",
    model = "gpt-oss-120b",
    generatedAt = Instant.parse("2026-09-14T12:00:00.123456Z"),
    promptTokens = 1_000,
    completionTokens = 200,
    totalTokens = 1_200,
    neurons = 75.5,
    generationDurationMs = 5_000,
)
