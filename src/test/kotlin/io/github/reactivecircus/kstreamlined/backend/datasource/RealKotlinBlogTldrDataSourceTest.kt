package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiException
import io.github.reactivecircus.kstreamlined.backend.cloudflare.successfulCloudflareAiResponse
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FakeKotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.FakeKotlinBlogTldrPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContent
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogContentPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrPersister
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldrSummary
import io.github.reactivecircus.kstreamlined.backend.datasource.persister.firestoreDocumentId
import io.github.reactivecircus.kstreamlined.backend.tldr.ModelConfig
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerationException
import io.github.reactivecircus.kstreamlined.backend.tldr.TldrGenerator
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
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

    private val tldrPersister = FakeKotlinBlogTldrPersister()

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
            tldrPersister = tldrPersister,
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertEquals(content, result.content)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to result), tldrPersister.savedKotlinBlogTldrs)
    }

    @Test
    fun `loadKotlinBlogTldr() returns a saved TLDR when present`() = runBlocking {
        tldrPersister.saveKotlinBlogTldr(article.guid, DummyKotlinBlogTldrSummary)
        var contentReads = 0
        var tldrWrites = 0
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    contentReads++
                    return contentPersister.loadKotlinBlogContent(id)
                }
            },
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
                    tldrWrites++
                    return tldrPersister.saveKotlinBlogTldr(id, tldr)
                }
            },
        )

        assertEquals(DummyKotlinBlogTldrSummary, dataSource.loadKotlinBlogTldr(article.guid))
        assertEquals(0, contentReads)
        assertEquals(0, tldrWrites)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() returns null when neither TLDR nor article content exists`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
        )

        assertNull(dataSource.loadKotlinBlogTldr(article.guid))
        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates saved TLDR lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
                    throw IOException("Summary read failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals("Summary read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates article content lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    throw IOException("Article read failed")
                }
            },
            tldrPersister = tldrPersister,
        )

        val failure = assertFailsWith<IOException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals("Article read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
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
                tldrPersister = tldrPersister,
            )

            assertFailsWith<IllegalArgumentException> { dataSource.loadKotlinBlogTldr(article.guid) }
        }

        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates HTTP failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
            engine = createEngine(status = HttpStatusCode.ServiceUnavailable),
        )

        val failure = assertFailsWith<ServerResponseException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals(HttpStatusCode.ServiceUnavailable, failure.response.status)
        assertEquals(1, requests.size)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() propagates Cloudflare response failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
            engine = createEngine(
                response = """{"result":null,"success":false,"errors":[{"code":10000,"message":"Rejected"}]}""",
            ),
        )

        assertFailsWith<CloudflareAiException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals(1, requests.size)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
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
                tldrPersister = tldrPersister,
                engine = createEngine(response = response),
            )
            assertFailsWith<TldrGenerationException> { dataSource.loadKotlinBlogTldr(article.guid) }
        }

        assertEquals(2, requests.size)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `loadKotlinBlogTldr() saves null usage metadata when usage is omitted`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
            engine = createEngine(response = successfulCloudflareAiResponse(includeUsage = false)),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertNull(result.promptTokens)
        assertNull(result.completionTokens)
        assertNull(result.totalTokens)
        assertNull(result.neurons)
        assertEquals(result, tldrPersister.loadKotlinBlogTldr(article.guid))
    }

    @Test
    fun `loadKotlinBlogTldr() saves null neurons when neurons are omitted`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
            engine = createEngine(response = successfulCloudflareAiResponse(includeNeurons = false)),
        )

        val result = dataSource.loadKotlinBlogTldr(article.guid)

        assertNotNull(result)
        assertNull(result.neurons)
        assertEquals(result, tldrPersister.loadKotlinBlogTldr(article.guid))
    }

    @Test
    fun `loadKotlinBlogTldr() propagates persistence failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
                    throw IOException("Summary save failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> { dataSource.loadKotlinBlogTldr(article.guid) }

        assertEquals("Summary save failed", failure.message)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() saves a new TLDR when none exists and persist is true`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(contentPersister, tldrPersister)

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = true)

        assertEquals(mapOf(article.guid.firestoreDocumentId to result), tldrPersister.savedKotlinBlogTldrs)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() returns a fresh TLDR without saving when persist is false`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        tldrPersister.saveKotlinBlogTldr(article.guid, DummyKotlinBlogTldrSummary)
        var tldrReads = 0
        var tldrWrites = 0
        val content = "Fresh TLDR."
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
                    tldrReads++
                    return tldrPersister.loadKotlinBlogTldr(id)
                }

                override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
                    tldrWrites++
                    tldrPersister.saveKotlinBlogTldr(id, tldr)
                }
            },
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = false)

        assertEquals(content, result.content)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to DummyKotlinBlogTldrSummary), tldrPersister.savedKotlinBlogTldrs)
        assertEquals(0, tldrReads)
        assertEquals(0, tldrWrites)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() replaces a saved TLDR when persist is true`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        tldrPersister.saveKotlinBlogTldr(article.guid, DummyKotlinBlogTldrSummary)
        var tldrReads = 0
        var tldrWrites = 0
        val content = "Fresh TLDR."
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
                    tldrReads++
                    return tldrPersister.loadKotlinBlogTldr(id)
                }

                override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
                    tldrWrites++
                    tldrPersister.saveKotlinBlogTldr(id, tldr)
                }
            },
            engine = createEngine(
                response = successfulCloudflareAiResponse(content = content),
                delay = 5.seconds,
            ),
        )

        val result = dataSource.createKotlinBlogTldr(id = article.guid, persist = true)

        assertEquals(content, result.content)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(generatedAt, result.generatedAt)
        assertNotNull(result.promptTokens)
        assertNotNull(result.completionTokens)
        assertNotNull(result.totalTokens)
        assertNotNull(result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to result), tldrPersister.savedKotlinBlogTldrs)
        assertEquals(0, tldrReads)
        assertEquals(1, tldrWrites)
        assertEquals(1, requests.size)
    }

    @Test
    fun `createKotlinBlogTldr() fails when article content is missing despite a saved TLDR`() = runBlocking {
        val dataSource = createDataSource(contentPersister, tldrPersister)
        tldrPersister.saveKotlinBlogTldr(article.guid, DummyKotlinBlogTldrSummary)

        val failure = assertFailsWith<IllegalStateException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = false)
        }

        assertEquals("Kotlin Blog content not found for article: ${article.guid}.", failure.message)
        assertEquals(DummyKotlinBlogTldrSummary, tldrPersister.loadKotlinBlogTldr(article.guid))
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() propagates article content lookup failures`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = object : KotlinBlogContentPersister by contentPersister {
                override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
                    throw IOException("Article read failed")
                }
            },
            tldrPersister = tldrPersister,
        )

        val failure = assertFailsWith<IOException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals("Article read failed", failure.message)
        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() propagates HTTP failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
            engine = createEngine(status = HttpStatusCode.ServiceUnavailable),
        )

        val failure = assertFailsWith<ServerResponseException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, failure.response.status)
        assertEquals(1, requests.size)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() rejects incomplete or blank model output`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val responses = listOf(
            successfulCloudflareAiResponse(finishReason = "length"),
            successfulCloudflareAiResponse(content = " "),
        )
        for (response in responses) {
            val dataSource = createDataSource(contentPersister, tldrPersister, createEngine(response))
            assertFailsWith<TldrGenerationException> {
                dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
            }
        }
        assertEquals(2, requests.size)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `createKotlinBlogTldr() propagates persistence failures`() = runBlocking {
        contentPersister.saveMissingKotlinBlogContents(listOf(article))
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = object : KotlinBlogTldrPersister by tldrPersister {
                override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
                    throw IOException("Summary save failed")
                }
            },
        )

        val failure = assertFailsWith<IOException> {
            dataSource.createKotlinBlogTldr(id = article.guid, persist = true)
        }

        assertEquals("Summary save failed", failure.message)
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
        assertEquals(1, requests.size)
    }

    private fun createDataSource(
        contentPersister: KotlinBlogContentPersister,
        tldrPersister: KotlinBlogTldrPersister,
        engine: MockEngine = createEngine(),
    ) = RealKotlinBlogTldrDataSource(
        kotlinBlogContentPersister = contentPersister,
        kotlinBlogTldrPersister = tldrPersister,
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
}
