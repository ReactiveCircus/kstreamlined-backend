package io.github.reactivecircus.kstreamlined.backend.tldr

class ModelConfig private constructor(
    val id: String,
    val providerModel: String,
    val temperature: Double,
    val topP: Double,
    val seed: Long,
    val maxTokens: Int,
    val reasoningEffort: ReasoningEffort?,
) {
    enum class ReasoningEffort {
        Low,
        Medium,
        High,
    }

    companion object {
        val GptOss120b = ModelConfig(
            id = "gpt-oss-120b",
            providerModel = "@cf/openai/gpt-oss-120b",
            temperature = 0.2,
            topP = 0.9,
            seed = 42,
            maxTokens = 1_500,
            reasoningEffort = ReasoningEffort.Low,
        )
    }
}
