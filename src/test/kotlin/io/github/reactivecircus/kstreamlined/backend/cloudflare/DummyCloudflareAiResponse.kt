package io.github.reactivecircus.kstreamlined.backend.cloudflare

import kotlinx.serialization.json.Json

fun successfulCloudflareAiResponse(
    content: String = "Generated TLDR.",
    returnedModel: String = "@cf/openai/gpt-oss-120b",
    choiceIndex: Int = 0,
    finishReason: String = "stop",
    includeUsage: Boolean = true,
    includeNeurons: Boolean = true,
): String {
    val usage = if (includeUsage) {
        """
        ,"usage": {
          "prompt_tokens": 1000,
          "completion_tokens": 200,
          "total_tokens": 1200
          ${if (includeNeurons) ""","neurons": 75.5""" else ""}
        }
        """.trimIndent()
    } else {
        ""
    }
    return """
        {
          "result": {
            "id": "completion-id",
            "object": "chat.completion",
            "created": 1757065600,
            "model": "$returnedModel",
            "choices": [
              {
                "index": $choiceIndex,
                "message": {
                  "role": "assistant",
                  "content": ${Json.encodeToString(content)}
                },
                "finish_reason": "$finishReason"
              }
            ]
            $usage
          },
          "success": true,
          "errors": []
        }
    """.trimIndent()
}
