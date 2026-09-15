package io.github.reactivecircus.kstreamlined.backend.tldr

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.cloudflare.successfulCloudflareAiResponse
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

class TldrGeneratorTest {
    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )

    private val timeSource = TestTimeSource()

    @Test
    fun `generate() sends expected prompt and model config via CloudFlareAiClient`() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(
                content = "  Generated TLDR.  ",
            ),
            requests = requests,
            delay = 10.seconds,
        )

        val result = generator.generate(
            title = "Structured Concurrency",
            articleText = "## What changed\n\nUse `coroutineScope`.",
        )

        val request = requests.single()
        assertTrue(request.url.toString().endsWith("/ai/run/@cf/openai/gpt-oss-120b"))
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        val messages = body.getValue("messages").jsonArray
        assertEquals("system", messages[0].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals(TldrPrompt.System, messages[0].jsonObject.getValue("content").jsonPrimitive.content)
        assertEquals("user", messages[1].jsonObject.getValue("role").jsonPrimitive.content)
        assertEquals(
            """
            Create the TLDR for this article in valid markdown. Decide what deserves emphasis and choose the clearest structure for this content.

            <UNTRUSTED_ARTICLE>
            Title: Structured Concurrency

            ## What changed

            Use `coroutineScope`.
            </UNTRUSTED_ARTICLE>
            """.trimIndent(),
            messages[1].jsonObject.getValue("content").jsonPrimitive.content,
        )
        assertEquals(0.2, body.getValue("temperature").jsonPrimitive.double)
        assertEquals(0.9, body.getValue("top_p").jsonPrimitive.double)
        assertEquals(42, body.getValue("seed").jsonPrimitive.int)
        assertEquals(1_500, body.getValue("max_tokens").jsonPrimitive.int)
        assertEquals("low", body.getValue("reasoning_effort").jsonPrimitive.content)

        assertEquals("Generated TLDR.", result.content)
        assertEquals(ModelConfig.GptOss120b.id, result.model)
        assertEquals(1_000, result.promptTokens)
        assertEquals(200, result.completionTokens)
        assertEquals(1_200, result.totalTokens)
        assertEquals(75.5, result.neurons)
        assertEquals(10_000, result.requestLatencyMs)
    }

    @Test
    fun `generate() rejects blank title or article text`() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(),
            requests = requests,
        )

        assertFailsWith<IllegalArgumentException> {
            generator.generate(title = " ", articleText = "Article")
        }
        assertFailsWith<IllegalArgumentException> {
            generator.generate(title = "Title", articleText = "\n")
        }

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `generate() rejects article text exceeding the maximum length`() = runBlocking {
        val requests = mutableListOf<HttpRequestData>()
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(),
            requests = requests,
        )

        assertFailsWith<IllegalArgumentException> {
            generator.generate(title = "Title", articleText = "a".repeat(40_001))
        }

        assertTrue(requests.isEmpty())
    }

    @Test
    fun `generate() rejects response without choice index zero`() = runBlocking {
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(choiceIndex = 1),
        )

        val exception = assertFailsWith<TldrGenerationException> {
            generator.generate(title = "Title", articleText = "Article")
        }

        assertEquals(
            "Cloudflare AI response must contain exactly one choice with index 0.",
            exception.message,
        )
    }

    @Test
    fun `generate() rejects incomplete response`() = runBlocking {
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(finishReason = "length"),
        )

        val exception = assertFailsWith<TldrGenerationException> {
            generator.generate(title = "Title", articleText = "Article")
        }

        assertEquals(
            "Cloudflare AI returned an incomplete TLDR (finish_reason=length).",
            exception.message,
        )
    }

    @Test
    fun `generate() rejects blank content`() = runBlocking {
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(content = " "),
        )

        val exception = assertFailsWith<TldrGenerationException> {
            generator.generate(title = "Title", articleText = "Article")
        }

        assertEquals("Cloudflare AI returned blank TLDR content.", exception.message)
    }

    @Test
    fun `generate() returns null usage fields when Cloudflare omits token usage`() = runBlocking {
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(includeUsage = false),
        )

        val result = generator.generate(title = "Title", articleText = "Article")

        assertNull(result.promptTokens)
        assertNull(result.completionTokens)
        assertNull(result.totalTokens)
        assertNull(result.neurons)
    }

    @Test
    fun `generate() returns null neurons when Cloudflare omits neuron usage`() = runBlocking {
        val generator = createGenerator(
            response = successfulCloudflareAiResponse(includeNeurons = false),
        )

        val result = generator.generate(title = "Title", articleText = "Article")

        assertEquals(1_000, result.promptTokens)
        assertEquals(200, result.completionTokens)
        assertEquals(1_200, result.totalTokens)
        assertNull(result.neurons)
    }

    private fun createGenerator(
        response: String,
        requests: MutableList<HttpRequestData> = mutableListOf(),
        delay: Duration = 0.milliseconds,
    ): TldrGenerator {
        val engine = MockEngine { request ->
            requests += request
            timeSource += delay
            respond(
                content = response,
                headers = jsonHeaders,
            )
        }
        return TldrGenerator(
            cloudflareAiClient = CloudflareAiClient(
                engine = engine,
                baseUrl = "https://api.cloudflare.com/client/v4",
                accountId = "account-id",
                apiToken = "api-token",
            ),
            timeSource = timeSource,
        )
    }
}
