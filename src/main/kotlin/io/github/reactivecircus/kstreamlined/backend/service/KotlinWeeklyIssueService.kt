package io.github.reactivecircus.kstreamlined.backend.service

import com.fleeksoft.ksoup.Ksoup
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntryGroup
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

interface KotlinWeeklyIssueService {
    suspend fun loadKotlinWeeklyIssue(url: String): List<KotlinWeeklyIssueEntry>
}

class RealKotlinWeeklyIssueService(
    engine: HttpClientEngine,
) : KotlinWeeklyIssueService {
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000L
            socketTimeoutMillis = 10_000L
        }
    }

    override suspend fun loadKotlinWeeklyIssue(url: String): List<KotlinWeeklyIssueEntry> {
        return buildList {
            val document = Ksoup.parse(httpClient.get(url).bodyAsText())
            document.select("div[style='overflow: hidden;']").forEach { section ->
                val divs = section.select("div").filter { it !== section }
                val groupName = divs.first().text().uppercase()
                val group = KotlinWeeklyIssueEntryGroup.entries.find { it.name == groupName } ?: return@forEach

                val titleWithLinkPairs = mutableListOf<Pair<String, String>>()
                val sources = mutableListOf<String>()
                val content = divs[1]

                content.select("a").forEach { anchor ->
                    if (!anchor.attr("style").contains("underline")) {
                        val href = anchor.attr("href")
                        if (anchor.children().any { it.hasAttr("style") }) {
                            titleWithLinkPairs.add(anchor.text() to href)
                        } else {
                            sources.add(href)
                        }
                    }
                }
                val summaries = content.select("span[style='font-size:14px']").map { it.text() }

                titleWithLinkPairs.forEachIndexed { index, pair ->
                    add(
                        KotlinWeeklyIssueEntry(
                            group = group,
                            title = pair.first,
                            url = pair.second,
                            summary = summaries[index],
                            source = sources[index],
                        ),
                    )
                }
            }
        }.deduplicate()
    }

    private fun List<KotlinWeeklyIssueEntry>.deduplicate(): List<KotlinWeeklyIssueEntry> {
        val seen = mutableSetOf<Pair<String, String>>()
        return filter { entry ->
            val duplicate = (entry.title to entry.url) in seen
            seen.add((entry.title to entry.url))
            !duplicate
        }
    }
}
