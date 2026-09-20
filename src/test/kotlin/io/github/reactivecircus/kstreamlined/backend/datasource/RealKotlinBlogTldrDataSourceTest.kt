package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiException
import io.github.reactivecircus.kstreamlined.backend.cloudflare.successfulCloudflareAiResponse
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FakeKotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.firestoreDocumentId
import io.github.reactivecircus.kstreamlined.backend.tldr.ModelConfig
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerationException
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.content.TextContent
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class RealKotlinBlogTldrDataSourceTest {
    private val contentPersister = FakeKotlinBlogContentPersister()

    private val requests = mutableListOf<HttpRequestData>()

    private val generatedAt = Instant.parse("2026-09-14T12:00:00Z")

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private val article = KotlinBlogItem(
        title = "Structured Concurrency",
        link = "https://blog.jetbrains.com/kotlin/structured-concurrency/",
        pubDate = "Mon, 14 Sep 2026 10:00:00 +0000",
        featuredImage = null,
        guid = "https://blog.jetbrains.com/?post_type=kotlin&p=12345",
        description = "An article about structured concurrency.",
        html = """
            <h2>What changed</h2>
            <p>Use <a href="https://kotlinlang.org/docs/coroutines-basics.html"><code>coroutineScope</code></a>.</p>
            <script>tracking()</script>
        """.trimIndent(),
    )

    private val timeSource = TestTimeSource()

    @Test
    fun `loadKotlinBlogTldr() generates and saves a TLDR when only article content exists`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val content = "**Use structured concurrency.**\n\n[Docs](https://kotlinlang.org/docs/coroutines-basics.html)"
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertEquals(content, result.output)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to result), contentPersister.allKotlinBlogTldrs)
    }

    @Test
    fun `loadKotlinBlogTldr() returns a saved TLDR when present`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        contentPersister.saveKotlinBlogTldrs(mapOf(article.guid to DummyKotlinBlogTldr))
        var contentReads = 0
        var tldrWrites = 0
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    contentReads++
                    return contentPersister.loadKotlinBlogContent(id)
                }

                override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
                    tldrWrites++
                    contentPersister.saveKotlinBlogTldrs(tldrs)
                }
            },
        )

        assertEquals(DummyKotlinBlogTldr, dataSource.loadKotlinBlogTldr(article.guid))
        assertEquals(1, contentReads)
        assertEquals(0, tldrWrites)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() returns null when article content does not exist`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = contentPersister,
        )

        assertNull(dataSource.loadKotlinBlogTldr(article.guid))
        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates article content lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    throw IOException("Article read failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals("Article read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() rejects invalid article input without calling AI`() = runBlocking {
        val invalidArticles = listOf(
            article.copy(title = " "),
            article.copy(html = "<script>Not article content</script>"),
            article.copy(html = "<p>${"a".repeat(40_001)}</p>"),
        )
        invalidArticles.forEach { invalidArticle ->
            val dataSource = createDataSource(
                contentPersister = FakeKotlinBlogContentPersister().apply {
                    saveMissingKotlinBlogContents(listOf(invalidArticle))
                },
            )

            assertFailsWith<IllegalArgumentException> { dataSource.loadKotlinBlogTldr(article.guid) }
        }

        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates HTTP failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(status = HttpStatusCode.ServiceUnavailable),
        )

        val failure = assertFailsWith<ServerResponseException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals(HttpStatusCode.ServiceUnavailable, failure.response.status)
        assertEquals(1, requests.size)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates Cloudflare response failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(
                response = """{"result":null,"success":false,"errors":[{"code":10000,"message":"Rejected"}]}""",
            ),
        )

        assertFailsWith<CloudflareAiException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals(1, requests.size)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() rejects incomplete or blank model output`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val responses = listOf(
            successfulCloudflareAiResponse(finishReason = "length"),
            successfulCloudflareAiResponse(content = " "),
        )
        responses.forEach { response ->
            val dataSource = createDataSource(
                contentPersister = contentPersister,
                engine = createEngine(response = response),
            )
            assertFailsWith<TldrGenerationException> { dataSource.loadKotlinBlogTldr(article.guid) }
        }

        assertEquals(2, requests.size)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() saves null usage metadata when usage is omitted`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(response = successfulCloudflareAiResponse(includeUsage = false)),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertNull(result.promptTokens)
        assertNull(result.completionTokens)
        assertNull(result.totalTokens)
        assertNull(result.neurons)
        assertEquals(result, contentPersister.loadKotlinBlogContent(article.guid)?.tldr)
    }

    @Test
    fun `loadKotlinBlogTldr() saves null neurons when neurons are omitted`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(response = successfulCloudflareAiResponse(includeNeurons = false)),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertNull(result.neurons)
        assertEquals(result, contentPersister.loadKotlinBlogContent(article.guid)?.tldr)
    }

    @Test
    fun `loadKotlinBlogTldr() propagates persistence failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
                    throw IOException("TLDR save failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals("TLDR save failed", failure.message)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() saves a new TLDR when none exists and persist is true`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(contentPersister)

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = true)

        assertEquals(mapOf(article.guid.firestoreDocumentId to result), contentPersister.allKotlinBlogTldrs)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() returns a fresh TLDR without saving when persist is false`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        contentPersister.saveKotlinBlogTldrs(mapOf(article.guid to DummyKotlinBlogTldr))
        var tldrWrites = 0
        val content = "Fresh TLDR."
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
                    tldrWrites++
                    contentPersister.saveKotlinBlogTldrs(tldrs)
                }
            },
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = false)

        assertEquals(content, result.output)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to DummyKotlinBlogTldr), contentPersister.allKotlinBlogTldrs)
        assertEquals(0, tldrWrites)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() replaces a saved TLDR when persist is true`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        contentPersister.saveKotlinBlogTldrs(mapOf(article.guid to DummyKotlinBlogTldr))
        var tldrWrites = 0
        val content = "Fresh TLDR."
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
                    tldrWrites++
                    contentPersister.saveKotlinBlogTldrs(tldrs)
                }
            },
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = true)

        assertEquals(content, result.output)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to result), contentPersister.allKotlinBlogTldrs)
        assertEquals(1, tldrWrites)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() propagates article content lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    throw IOException("Article read failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals("Article read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() propagates HTTP failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(status = HttpStatusCode.ServiceUnavailable),
        )

        val failure = assertFailsWith<ServerResponseException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, failure.response.status)
        assertEquals(1, requests.size)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() rejects incomplete or blank model output`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val responses = listOf(
            successfulCloudflareAiResponse(finishReason = "length"),
            successfulCloudflareAiResponse(content = " "),
        )
        for (response in responses) {
            val dataSource = createDataSource(contentPersister, createEngine(response))
            assertFailsWith<TldrGenerationException> {
                dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
            }
        }
        assertEquals(2, requests.size)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() propagates persistence failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
                    throw IOException("TLDR save failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals("TLDR save failed", failure.message)
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
        assertEquals(1, requests.size)
    }

    @Test
    fun `backfillKotlinBlogTldrs() skips generation when no article content exists`() = runBlocking {
        val dataSource = createDataSource(contentPersister = contentPersister)
        val result = dataSource.backfillKotlinBlogTldrs()

        assertEquals(KotlinBlogTldrBackfillResult(generatedCount = 0, failedIds = emptyList()), result)
        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `backfillKotlinBlogTldrs() generates TLDRs for article contents without existing TLDR`() = runBlocking {
        val articles = (1..3).map { index ->
            article.copy(
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=1234$index",
                title = "Article $index",
            )
        }
        contentPersister.saveMissingKotlinBlogContents(articles)
        contentPersister.saveKotlinBlogTldrs(mapOf(articles[0].guid to DummyKotlinBlogTldr))
        val dataSource = createDataSource(contentPersister = contentPersister)

        val result = dataSource.backfillKotlinBlogTldrs()

        assertEquals(KotlinBlogTldrBackfillResult(generatedCount = 2, failedIds = emptyList()), result)
        assertEquals(2, requests.size)
        assertEquals(3, contentPersister.allKotlinBlogTldrs.size)
    }

    @Test
    fun `backfillKotlinBlogTldrs() propagates article contents lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContentsWithoutTldr(): List<KotlinBlogContent> {
                    throw IOException("Articles read failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> { dataSource.backfillKotlinBlogTldrs() }

        assertEquals("Articles read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(contentPersister.allKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `backfillKotlinBlogTldrs() reports per-article generation failures and saves generated TLDRs`() = runBlocking {
        val articles = (1..3).map { index ->
            article.copy(
                guid = "https://blog.jetbrains.com/?post_type=kotlin&p=article$index",
                title = "Article $index",
            )
        }
        contentPersister.saveMissingKotlinBlogContents(articles)
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            engine = createEngine(
                response = { request ->
                    // 1st and 3rd requests fails, 2nd request succeeds
                    if ((request.body as TextContent).text.matches(Regex(".*Article [13].*"))) {
                        """{"result":null,"success":false,"errors":[{"code":10000,"message":"Rejected"}]}"""
                    } else {
                        successfulCloudflareAiResponse(content = "TLDR")
                    }
                },
            ),
        )

        val result = dataSource.backfillKotlinBlogTldrs()

        assertEquals(
            KotlinBlogTldrBackfillResult(
                generatedCount = 1,
                failedIds = listOf(articles[0].guid, articles[2].guid),
            ),
            result,
        )
        assertEquals(3, requests.size)
        assertEquals(1, contentPersister.allKotlinBlogTldrs.size)
    }

    private fun createDataSource(
        contentPersister: KotlinBlogContentPersister,
        engine: MockEngine = createEngine(),
    ) = RealKotlinBlogTldrDataSource(
        kotlinBlogContentPersister = contentPersister,
        tldrGenerator = TldrGenerator(
            cloudflareAiClient = CloudflareAiClient(
                engine = engine,
                baseUrl = "https://api.cloudflare.com/client/v4",
                accountId = "account-id",
                apiToken = "api-token",
            ),
            timeSource = timeSource,
        ),
        clock = Clock.fixed(generatedAt, ZoneOffset.UTC),
    )

    private fun createEngine(
        response: String = successfulCloudflareAiResponse(),
        status: HttpStatusCode = HttpStatusCode.OK,
        delay: Duration = 0.milliseconds,
    ) = MockEngine { request ->
        requests += request
        timeSource += delay
        respond(content = response, status = status, headers = jsonHeaders)
    }

    private fun createEngine(
        response: (HttpRequestData) -> String,
        delay: Duration = 0.milliseconds,
    ) = MockEngine { request ->
        requests += request
        timeSource += delay
        respond(content = response(request), headers = jsonHeaders)
    }
}
