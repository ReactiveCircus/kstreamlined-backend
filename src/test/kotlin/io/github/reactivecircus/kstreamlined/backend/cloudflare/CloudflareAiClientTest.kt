package io.github.reactivecircus.kstreamlined.backend.cloudflare

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
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

class CloudflareAiClientTest {
    private val dummyRequest = CloudflareAiRequest(
        messages = listOf(
            CloudflareAiRequest.Message(CloudflareAiRequest.Message.Role.User, "Article"),
        ),
        temperature = 0.2,
        topP = 0.9,
        seed = 42,
        maxTokens = 1_500,
        reasoningEffort = CloudflareAiRequest.ReasoningEffort.Medium,
    )

    private val jsonHeaders = headersOf(
        HttpHeaders.ContentType,
        ContentType.Application.Json.toString(),
    )

    val successfulResponse = """
        {
          "result": {
            "id": "completion-id",
            "object": "chat.completion",
            "created": 1757065600,
            "model": "@cf/openai/gpt-oss-120b",
            "choices": [
              {
                "index": 0,
                "message": {
                  "role": "assistant",
                  "content": "Generated summary"
                },
                "finish_reason": "stop"
              }
            ],
            "usage": {
              "prompt_tokens": 1000,
              "completion_tokens": 250,
              "total_tokens": 1250,
              "neurons": 82.75
            }
          },
          "success": true,
          "errors": [],
          "messages": []
        }
    """.trimIndent()

    @Test
    fun `run() sends the request and returns expected CloudflareAiResult when API call succeeds`() = runBlocking {
        val recordedRequestData = mutableListOf<HttpRequestData>()
        val mockEngine = MockEngine { request ->
            recordedRequestData.add(request)
            respond(
                content = successfulResponse,
                headers = jsonHeaders,
            )
        }
        val client = createClient(mockEngine)

        val result = client.run("@cf/openai/gpt-oss-120b", dummyRequest)

        val request = recordedRequestData.single()
        assertEquals(
            "https://api.cloudflare.com/client/v4/accounts/account-id/ai/run/@cf/openai/gpt-oss-120b",
            request.url.toString(),
        )
        val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        val message = body.getValue("messages").jsonArray.single().jsonObject
        assertEquals("user", message.getValue("role").jsonPrimitive.content)
        assertEquals("Article", message.getValue("content").jsonPrimitive.content)
        assertEquals(0.2, body.getValue("temperature").jsonPrimitive.double)
        assertEquals(0.9, body.getValue("top_p").jsonPrimitive.double)
        assertEquals(42, body.getValue("seed").jsonPrimitive.int)
        assertEquals(1_500, body.getValue("max_tokens").jsonPrimitive.int)
        assertEquals("medium", body.getValue("reasoning_effort").jsonPrimitive.content)

        assertEquals("completion-id", result.id)
        assertEquals("chat.completion", result.objectType)
        assertEquals(1_757_065_600, result.created)
        assertEquals("@cf/openai/gpt-oss-120b", result.model)
        assertEquals("Generated summary", result.choices.single().message.content)
        assertEquals("stop", result.choices.single().finishReason)
        assertEquals(82.75, result.usage?.neurons)
    }

    @Test
    fun `run() propagates HTTP failures`() = runBlocking {
        val client = createClient(
            MockEngine {
                respondError(HttpStatusCode.ServiceUnavailable)
            },
        )

        val exception = assertFailsWith<ServerResponseException> {
            client.run("@cf/openai/gpt-oss-120b", dummyRequest)
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, exception.response.status)
    }

    @Test
    fun `run() throws CloudflareAiException when response contains errors`() = runBlocking {
        val client = createClient(
            MockEngine {
                respond(
                    content = """
                        {
                          "result": null,
                          "success": false,
                          "errors": [
                            {
                              "code": 10000,
                              "message": "Authentication error for account-id"
                            }
                          ]
                        }
                    """.trimIndent(),
                    headers = jsonHeaders,
                )
            },
        )

        val exception = assertFailsWith<CloudflareAiException> {
            client.run("@cf/openai/gpt-oss-120b", dummyRequest)
        }

        assertEquals(
            "Cloudflare Workers AI rejected the request. Error codes: 10000.",
            exception.message,
        )
    }

    @Test
    fun `run() throws CloudflareAiException when successful response has no result`() = runBlocking {
        val client = createClient(
            MockEngine {
                respond(
                    content = """
                        {
                          "result": null,
                          "success": true,
                          "errors": []
                        }
                    """.trimIndent(),
                    headers = jsonHeaders,
                )
            },
        )

        val exception = assertFailsWith<CloudflareAiException> {
            client.run("@cf/openai/gpt-oss-120b", dummyRequest)
        }

        assertEquals(
            "Cloudflare Workers AI response did not contain a result.",
            exception.message,
        )
    }

    private fun createClient(mockEngine: MockEngine) = CloudflareAiClient(
        engine = mockEngine,
        baseUrl = "https://api.cloudflare.com/client/v4",
        accountId = "account-id",
        apiToken = "api-token",
    )
}
