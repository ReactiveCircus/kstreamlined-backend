package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntryGroup
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import it.skrape.core.htmlDocument
import it.skrape.selects.eachText
import it.skrape.selects.html5.a
import it.skrape.selects.html5.div
import it.skrape.selects.html5.span

interface KotlinWeeklyIssueDataSource {
    suspend fun loadKotlinWeeklyIssue(url: String): List<KotlinWeeklyIssueEntry>
}

class RealKotlinWeeklyIssueDataSource(
    engine: HttpClientEngine,
) : KotlinWeeklyIssueDataSource {
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(HttpTimeout) {
            connectTimeoutMillis = HttpTimeoutMillis
            requestTimeoutMillis = HttpTimeoutMillis
        }
    }

    override suspend fun loadKotlinWeeklyIssue(url: String): List<KotlinWeeklyIssueEntry> {
        return buildList {
            htmlDocument(httpClient.get(url).bodyAsText()) {
                div {
                    withAttribute = "style" to "overflow: hidden;"
                    findAll {
                        forEach { section ->
                            val group = section.div {
                                findFirst { text }
                            }.uppercase().let {
                                KotlinWeeklyIssueEntryGroup.entries.find { type -> it == type.name } ?: return@forEach
                            }

                            val titleWithLinkPairs = mutableListOf<Pair<String, String>>()
                            val summaries = mutableListOf<String>()
                            val sources = mutableListOf<String>()

                            section.div {
                                findSecond {
                                    a {
                                        findAll {
                                            forEach {
                                                if (!it.attribute("style").contains("underline")) {
                                                    it.eachHref.first().let { url ->
                                                        if (it.children.any { it.hasAttribute("style") }) {
                                                            titleWithLinkPairs.add(it.text to url)
                                                        } else {
                                                            sources.add(url)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    span {
                                        withAttribute = "style" to "font-size:14px"
                                        findAll {
                                            eachText.forEach {
                                                summaries.add(it)
                                            }
                                        }
                                    }
                                }
                            }

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
                    }
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

    companion object {
        private const val HttpTimeoutMillis = 10_000L
    }
}
