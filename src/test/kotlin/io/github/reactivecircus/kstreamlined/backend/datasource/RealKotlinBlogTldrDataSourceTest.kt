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
    fun `generates from extracted article text and persists the result with metadata`() = runBlocking {
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
        assertEquals(1_000, result.promptTokens)
        assertEquals(200, result.completionTokens)
        assertEquals(1_200, result.totalTokens)
        assertEquals(75.5, result.neurons)
        assertEquals(5_000, result.generationDurationMs)
        assertEquals(mapOf(article.guid.firestoreDocumentId to result), tldrPersister.savedKotlinBlogTldrs)
    }

    @Test
    fun `returns a saved TLDR without loading article content generating or writing`() = runBlocking {
        val saved = KotlinBlogTldrSummary(
            content = "Previously generated TLDR.",
            model = "gpt-oss-120b",
            generatedAt = generatedAt.minusSeconds(60),
            promptTokens = null,
            completionTokens = null,
            totalTokens = null,
            neurons = null,
            generationDurationMs = 2_500,
        )
        tldrPersister.saveKotlinBlogTldr(article.guid, saved)
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

        assertEquals(saved, dataSource.loadKotlinBlogTldr(article.guid))
        assertEquals(0, contentReads)
        assertEquals(0, tldrWrites)
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `missing article content returns null without generation`() = runBlocking {
        val dataSource = createDataSource(
            contentPersister = contentPersister,
            tldrPersister = tldrPersister,
        )

        assertNull(dataSource.loadKotlinBlogTldr(article.guid))
        assertTrue(requests.isEmpty())
        assertTrue(tldrPersister.savedKotlinBlogTldrs.isEmpty())
    }

    @Test
    fun `summary lookup failure propagates without reading content or generating`() = runBlocking {
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
    fun `article lookup failure propagates without generating`() = runBlocking {
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
    fun `invalid title empty extraction and oversized article fail before the AI call`() = runBlocking {
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
    fun `HTTP failures propagate without saving or automatically retrying`() = runBlocking {
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
    fun `Cloudflare envelope failures propagate without saving`() = runBlocking {
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
    fun `incomplete and blank model output are not persisted`() = runBlocking {
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
    fun `missing usage is persisted as null metadata`() = runBlocking {
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
    fun `missing neurons is persisted as null`() = runBlocking {
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
    fun `save failure propagates`() = runBlocking {
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
