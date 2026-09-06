package io.github.reactivecircus.kstreamlined.backend.cloudflare

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class CloudflareAiClient(
    engine: HttpClientEngine,
    private val baseUrl: String,
    private val accountId: String,
    private val apiToken: String,
) {
    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(CloudflareAiJson)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = RequestTimeoutMillis
        }
    }

    suspend fun run(
        model: String,
        request: CloudflareAiRequest,
    ): CloudflareAiResult {
        val response = httpClient.post("$baseUrl/accounts/$accountId/ai/run/$model") {
            bearerAuth(apiToken)
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<CloudflareAiResponse>()

        if (!response.success || response.errors.isNotEmpty()) {
            val errorCodes = response.errors
                .map(CloudflareAiError::code)
                .distinct()
                .joinToString()
            val errorSuffix = errorCodes.takeIf(String::isNotEmpty)?.let { " Error codes: $it." }.orEmpty()
            throw CloudflareAiException("Cloudflare Workers AI rejected the request.$errorSuffix")
        }
        return response.result
            ?: throw CloudflareAiException("Cloudflare Workers AI response did not contain a result.")
    }

    private companion object {
        const val RequestTimeoutMillis = 60_000L
    }
}

@Serializable
data class CloudflareAiRequest(
    val messages: List<Message>,
    val temperature: Double,
    @SerialName("top_p")
    val topP: Double,
    val seed: Long,
    @SerialName("max_tokens")
    val maxTokens: Int,
    @SerialName("reasoning_effort")
    val reasoningEffort: ReasoningEffort? = null,
) {
    @Serializable
    data class Message(
        val role: Role,
        val content: String,
    ) {
        @Serializable
        enum class Role {
            @SerialName("system")
            System,

            @SerialName("user")
            User,

            @SerialName("assistant")
            Assistant,
        }
    }

    @Serializable
    enum class ReasoningEffort {
        @SerialName("low")
        Low,

        @SerialName("medium")
        Medium,

        @SerialName("high")
        High,
    }
}

@Serializable
private data class CloudflareAiResponse(
    val result: CloudflareAiResult?,
    val success: Boolean,
    val errors: List<CloudflareAiError>,
)

@Serializable
private data class CloudflareAiError(
    val code: Int,
    val message: String,
)

@Serializable
data class CloudflareAiResult(
    val id: String,
    @SerialName("object")
    val objectType: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage? = null,
) {
    @Serializable
    data class Choice(
        val index: Int,
        val message: Message,
        @SerialName("finish_reason")
        val finishReason: String,
    ) {
        @Serializable
        data class Message(
            val role: String,
            val content: String?,
        )
    }

    @Serializable
    data class Usage(
        @SerialName("prompt_tokens")
        val promptTokens: Int,
        @SerialName("completion_tokens")
        val completionTokens: Int,
        @SerialName("total_tokens")
        val totalTokens: Int,
        val neurons: Double? = null,
    )
}

class CloudflareAiException(
    message: String,
) : RuntimeException(message)

private val CloudflareAiJson = Json {
    ignoreUnknownKeys = true
}
