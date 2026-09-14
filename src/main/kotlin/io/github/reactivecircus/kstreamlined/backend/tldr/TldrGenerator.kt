package io.github.reactivecircus.kstreamlined.backend.tldr

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiClient
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiResult
import kotlin.time.measureTimedValue

class TldrGenerator(
    private val cloudflareAiClient: CloudflareAiClient,
    private val modelConfig: ModelConfig = ModelConfig.GptOss120b,
) {
    suspend fun generate(
        title: String,
        articleText: String,
    ): TldrGenerationResult {
        require(title.isNotBlank()) { "Article title must not be blank." }
        require(articleText.isNotBlank()) { "Article text must not be blank." }
        require(articleText.length <= MaxArticleTextLength) {
            "Article text must not exceed $MaxArticleTextLength characters (was ${articleText.length})."
        }

        val (result, duration) = measureTimedValue {
            cloudflareAiClient.run(
                model = modelConfig.providerModel,
                request = CloudflareAiRequest(
                    messages = listOf(
                        CloudflareAiRequest.Message(
                            role = CloudflareAiRequest.Message.Role.System,
                            content = TldrPrompt.System,
                        ),
                        CloudflareAiRequest.Message(
                            role = CloudflareAiRequest.Message.Role.User,
                            content = TldrPrompt.user(title = title, articleText = articleText),
                        ),
                    ),
                    temperature = modelConfig.temperature,
                    topP = modelConfig.topP,
                    seed = modelConfig.seed,
                    maxTokens = modelConfig.maxTokens,
                    reasoningEffort = modelConfig.reasoningEffort.toCloudflareReasoningEffort(),
                ),
            )
        }

        val choice = result.requirePrimaryChoice()
        if (choice.finishReason != "stop") {
            throw TldrGenerationException(
                "Cloudflare AI returned an incomplete TLDR (finish_reason=${choice.finishReason}).",
            )
        }
        val content = choice.requireContent()
        val usage = result.usage

        return TldrGenerationResult(
            content = content,
            model = modelConfig.id,
            promptTokens = usage?.promptTokens,
            completionTokens = usage?.completionTokens,
            totalTokens = usage?.totalTokens,
            neurons = usage?.neurons,
            requestLatencyMs = duration.inWholeMilliseconds,
        )
    }

    private companion object {
        const val MaxArticleTextLength = 40_000
    }
}

private fun CloudflareAiResult.requirePrimaryChoice(): CloudflareAiResult.Choice {
    return choices.singleOrNull { it.index == 0 }
        ?: throw TldrGenerationException(
            "Cloudflare AI response must contain exactly one choice with index 0.",
        )
}

private fun CloudflareAiResult.Choice.requireContent(): String {
    return message.content?.trim()
        ?.takeIf(String::isNotEmpty)
        ?: throw TldrGenerationException("Cloudflare AI returned blank TLDR content.")
}

data class TldrGenerationResult(
    val content: String,
    val model: String,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val neurons: Double?,
    val requestLatencyMs: Long,
)

class TldrGenerationException(
    message: String,
) : RuntimeException(message)

private fun ModelConfig.ReasoningEffort?.toCloudflareReasoningEffort(): CloudflareAiRequest.ReasoningEffort? {
    return when (this) {
        ModelConfig.ReasoningEffort.Low -> CloudflareAiRequest.ReasoningEffort.Low
        ModelConfig.ReasoningEffort.Medium -> CloudflareAiRequest.ReasoningEffort.Medium
        ModelConfig.ReasoningEffort.High -> CloudflareAiRequest.ReasoningEffort.High
        null -> null
    }
}
