package io.github.reactivecircus.kstreamlined.backend.integration

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class ServiceHttpStubs : AutoCloseable {
    enum class AiResponse { Success, Rejected, HttpFailure }

    private val redis = ConcurrentHashMap<String, String>()
    private val recordedRequests = CopyOnWriteArrayList<RecordedRequest>()
    val unexpectedRequests = CopyOnWriteArrayList<RecordedRequest>()
    private val fixtures = mapOf(
        "/feeds/blog" to ("kotlin_blog_rss_response_sample.xml" to "application/rss+xml"),
        "/feeds/youtube" to ("kotlin_youtube_rss_response_sample.xml" to "application/xml"),
        "/feeds/podcast" to ("talking_kotlin_rss_response_sample.xml" to "application/rss+xml"),
        "/feeds/weekly" to ("kotlin_weekly_rss_response_sample.xml" to "application/rss+xml"),
        "/weekly-issue" to ("kotlin_weekly_issue_sample.html" to "text/html"),
    ).mapValues { (_, fixture) -> resource(fixture.first) to fixture.second }
    private val aiSuccess = resource("http-stubs/cloudflare-success.json")
    private val aiError = resource("http-stubs/cloudflare-error.json")
    private val server = MockWebServer()

    @Volatile
    var aiResponse = AiResponse.Success

    @Volatile
    var rejectedTitle: String? = null

    @Volatile
    var failedFeed: String? = null

    val baseUrl: String get() = server.url("/").toString().removeSuffix("/")

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

    fun reset() {
        redis.clear()
        recordedRequests.clear()
        unexpectedRequests.clear()
        aiResponse = AiResponse.Success
        rejectedTitle = null
        failedFeed = null
    }

    override fun close() {
        server.close()
    }

    private fun respond(request: RecordedRequest): MockResponse {
        val path = request.url.encodedPath
        return when {
            path == failedFeed -> response(503, "text/plain", "Feed unavailable")

            fixtures.containsKey(path) && request.method == "GET" ->
                fixtures.getValue(path).let { (body, type) -> response(200, type, body) }

            path.startsWith("/redis/") -> redisResponse(request)

            path == "/ai/accounts/integration/ai/run/@cf/openai/gpt-oss-120b" && request.method == "POST" ->
                aiResponse(request)

            else -> unexpectedRequest(request)
        }
    }

    private fun redisResponse(request: RecordedRequest): MockResponse {
        val segments = request.url.pathSegments
        val key = segments.last()
        val result = when {
            segments.getOrNull(1) == "get" && request.method == "GET" -> redis[key]

            segments.getOrNull(1) == "set" && request.method == "POST" -> {
                redis[key] = checkNotNull(request.body).utf8()
                "OK"
            }

            else -> return unexpectedRequest(request)
        }
        return response(200, "application/json", buildJsonObject { put("result", result) }.toString())
    }

    private fun unexpectedRequest(request: RecordedRequest): MockResponse {
        unexpectedRequests.add(request)
        return response(404, "text/plain", "Unconfigured stub: ${request.method} ${request.url.encodedPath}")
    }

    private fun aiResponse(request: RecordedRequest): MockResponse {
        val reject = rejectedTitle?.let { checkNotNull(request.body).utf8().contains(it) } == true
        return when {
            aiResponse == AiResponse.HttpFailure -> response(503, "application/json", aiError)
            aiResponse == AiResponse.Rejected || reject -> response(200, "application/json", aiError)
            else -> response(200, "application/json", aiSuccess)
        }
    }

    private fun response(status: Int, contentType: String, body: String) = MockResponse.Builder()
        .code(status)
        .addHeader("Content-Type", "$contentType; charset=utf-8")
        .body(body)
        .build()

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader.getResource(name)) { "Missing integration fixture: $name." }.readText()
}
