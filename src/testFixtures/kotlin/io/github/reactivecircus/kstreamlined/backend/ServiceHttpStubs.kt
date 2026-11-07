package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicReference

class ServiceHttpStubs : AutoCloseable {
    enum class AiResponse { Success, Rejected, HttpFailure }

    private val redis = ConcurrentHashMap<String, String>()
    private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()
    private val unexpectedRequestLog = CopyOnWriteArrayList<RecordedRequest>()
    private val feedFailures = ConcurrentHashMap<String, Int>()
    private val aiResponses = AtomicReference(AiResponses())
    private val fixtures = mapOf(
        "/feeds/blog" to ("kotlin_blog_rss_response_sample.xml" to "application/rss+xml"),
        "/feeds/youtube" to ("kotlin_youtube_api_response_sample.json" to "application/json"),
        "/feeds/podcast" to ("talking_kotlin_rss_response_sample.xml" to "application/rss+xml"),
        "/feeds/weekly" to ("kotlin_weekly_rss_response_sample.xml" to "application/rss+xml"),
        "/weekly-issue" to ("kotlin_weekly_issue_sample.html" to "text/html"),
    ).mapValues { (_, fixture) -> resource(fixture.first) to fixture.second }
    private val aiSuccess = resource("http-stubs/cloudflare-success.json")
    private val aiError = resource("http-stubs/cloudflare-error.json")
    private val server = MockWebServer()

    val baseUrl: String get() = server.url("/").toString().removeSuffix("/")
    val unexpectedRequests: List<RecordedRequest> get() = unexpectedRequestLog.toList()

    init {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                recordedRequests.add(request)
                return respond(request)
            }
        }
        server.start(InetAddress.getByName("127.0.0.1"), 0)
    }

    fun requests(pathPrefix: String): List<RecordedRequest> {
        return recordedRequests.filter { it.url.encodedPath.startsWith(pathPrefix) }
    }

    fun seedRedis(key: String, resourceName: String) {
        redis[key] = resource(resourceName)
    }

    fun stubFeedFailure(path: String, status: Int = 503) {
        require(path.startsWith("/feeds/") && path in fixtures) { "Unknown feed route: $path." }
        require(status in 400..599) { "Feed failure status must be in 400..599, not $status." }
        feedFailures[path] = status
    }

    fun stubAiResponse(response: AiResponse) {
        aiResponses.updateAndGet { it.copy(defaultResponse = response) }
    }

    fun stubAiResponse(response: AiResponse, matches: (CloudflareAiRequest) -> Boolean) {
        aiResponses.updateAndGet { it.copy(overrides = it.overrides + AiStub(response, matches)) }
    }

    fun reset() {
        redis.clear()
        recordedRequests.clear()
        unexpectedRequestLog.clear()
        feedFailures.clear()
        aiResponses.set(AiResponses())
    }

    override fun close() {
        server.close()
    }

    private fun respond(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path == "/feeds/youtube" && request.headers[YouTubeApiKeyHeader] != YouTubeApiKey -> {
                unexpectedRequest(request, 403, "Missing or invalid YouTube API key.")
            }

            fixtures.containsKey(path) && request.method == "GET" -> {
                val status = feedFailures[path]
                if (status != null) {
                    response(status, "text/plain", "Feed unavailable")
                } else {
                    fixtures.getValue(path).let { (body, type) -> response(200, type, body) }
                }
            }

            path == "/redis" || path.startsWith("/redis/") -> redisResponse(request)

            path == "/ai/accounts/integration/ai/run/@cf/openai/gpt-oss-120b" && request.method == "POST" -> aiResponse(request)

            else -> unexpectedRequest(request)
        }
    }

    private fun redisResponse(request: RecordedRequest): MockResponse {
        val segments = request.url.pathSegments.filter { it.isNotEmpty() }
        return when {
            segments.getOrNull(1) == "get" && request.method == "GET" -> {
                response(200, "application/json", buildJsonObject { put("result", redis[segments.last()]) }.toString())
            }

            segments.size == 1 && request.method == "POST" -> redisEvalResponse(request)

            segments.getOrNull(1) == "multi-exec" && request.method == "POST" -> redisMultiExecResponse(request)

            else -> unexpectedRequest(request)
        }
    }

    /**
     * Stubs the script sent by `RedisClient.setIfChanged`:
     * `EVAL <script> 2 <key> <checksumKey> <checksum> <expiry>` returns 1 if `<key>` exists and its checksum matches.
     */
    private fun redisEvalResponse(request: RecordedRequest): MockResponse {
        val command = Json.parseToJsonElement(checkNotNull(request.body).utf8()).jsonArray
        val (key, checksumKey, checksum) = command.subList(3, 6).map { it.jsonPrimitive.content }
        val matches = redis.containsKey(key) && redis[checksumKey] == checksum
        return response(200, "application/json", """{"result":${if (matches) 1 else 0}}""")
    }

    private fun redisMultiExecResponse(request: RecordedRequest): MockResponse {
        val commands = Json.parseToJsonElement(checkNotNull(request.body).utf8()).jsonArray.map { command ->
            command.jsonArray.map { it.jsonPrimitive.content }
        }
        // SET <key> <value> EX <expiry>
        commands.forEach { (_, key, value) -> redis[key] = value }
        return response(200, "application/json", commands.joinToString(",", "[", "]") { """{"result":"OK"}""" })
    }

    private fun unexpectedRequest(
        request: RecordedRequest,
        status: Int = 404,
        message: String = "Unconfigured stub: ${request.method} ${request.url.encodedPath}",
    ): MockResponse {
        unexpectedRequestLog.add(request)
        return response(status, "text/plain", message)
    }

    private fun aiResponse(request: RecordedRequest): MockResponse {
        val body = request.body
        return if (body == null) {
            unexpectedRequest(request, 400, "Missing AI request body.")
        } else {
            val decoded = try {
                Json.decodeFromString<CloudflareAiRequest>(body.utf8())
            } catch (failure: SerializationException) {
                return unexpectedRequest(request, 400, "Invalid AI request: ${failure.message}")
            }
            val responses = aiResponses.get()
            val result = responses.overrides.lastOrNull { it.matches(decoded) }?.response ?: responses.defaultResponse
            when (result) {
                AiResponse.HttpFailure -> response(503, "application/json", aiError)
                AiResponse.Rejected -> response(200, "application/json", aiError)
                AiResponse.Success -> response(200, "application/json", aiSuccess)
            }
        }
    }

    private fun response(status: Int, contentType: String, body: String) = MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "$contentType; charset=utf-8")
        .body(body)
        .build()

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader.getResource(name)) { "Missing test fixture: $name." }.readText()

    companion object {
        const val YouTubeApiKey = "test-youtube-api-key"
        private const val YouTubeApiKeyHeader = "X-Goog-Api-Key"
    }

    private data class AiStub(
        val response: AiResponse,
        val matches: (CloudflareAiRequest) -> Boolean,
    )

    private data class AiResponses(
        val defaultResponse: AiResponse = AiResponse.Success,
        val overrides: List<AiStub> = emptyList(),
    )
}
