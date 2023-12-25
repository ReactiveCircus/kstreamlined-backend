package io.github.reactivecircus.kstreamlined.backend.client

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntryType
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

interface KotlinWeeklyIssueClient {
    suspend fun loadKotlinWeeklyIssue(url: String): List<KotlinWeeklyIssueEntry>
}

class RealKotlinWeeklyIssueClient(
    engine: HttpClientEngine,
) : KotlinWeeklyIssueClient {

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
                            val type = section.div {
                                findFirst { text }
                            }.uppercase().let {
                                KotlinWeeklyIssueEntryType.entries.find { type -> it == type.name } ?: return@forEach
                            }

                            val titleWithLinkPairs = mutableListOf<Pair<String, String>>()
                            val summaries = mutableListOf<String>()
                            val sources = mutableListOf<String>()

                            section.div {
                                findSecond {
                                    a {
                                        findAll {
                                            forEach {
                                                it.eachHref.first().let { url ->
                                                    if (it.text != url) {
                                                        titleWithLinkPairs.add(it.text to url)
                                                    } else {
                                                        sources.add(url)
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
                                        type = type,
                                        title = pair.first,
                                        url = pair.second,
                                        summary = summaries[index],
                                        source = sources[index],
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val HttpTimeoutMillis = 10_000L
    }
}
