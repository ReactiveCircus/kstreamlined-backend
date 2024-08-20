package io.github.reactivecircus.kstreamlined.backend.redis

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import org.slf4j.LoggerFactory

class RedisClient(
    engine: HttpClientEngine,
    private val url: String,
    private val token: String,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    private val httpClient = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(DefaultJson)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = HttpTimeoutMillis
            requestTimeoutMillis = HttpTimeoutMillis
        }
    }

    suspend fun get(key: String): String? {
        return executeWithErrorHandling(operation = "GET", key = key) {
            httpClient.get("$url/get/$key") {
                bearerAuth(token)
            }.bodyAsText().let { result ->
                DefaultJson.decodeFromString<Map<String, String?>>(result)["result"]
            }
        }
    }

    suspend fun set(key: String, value: String, keyExpirySeconds: Int = DefaultKeyExpirySeconds) {
        executeWithErrorHandling(operation = "SET", key = key) {
            httpClient.post("$url/set/$key") {
                parameter("EX", keyExpirySeconds)
                contentType(ContentType.Application.Json)
                setBody(value)
                bearerAuth(token)
            }
        }
    }

    private suspend fun <T> executeWithErrorHandling(
        operation: String,
        key: String,
        block: suspend () -> T,
    ): T? {
        return try {
            block()
        } catch (e: ClientRequestException) {
            logger.error("Client error during Redis $operation operation for key: $key", e)
            null
        } catch (e: ServerResponseException) {
            logger.error("Server error during Redis $operation operation for key: $key", e)
            null
        } catch (e: HttpRequestTimeoutException) {
            logger.error("Timeout during Redis $operation operation for key: $key", e)
            null
        } catch (e: ResponseException) {
            logger.error("Unexpected response during Redis $operation operation for key: $key", e)
            null
        }
    }

    companion object {
        private const val HttpTimeoutMillis = 5_000L
        private const val DefaultKeyExpirySeconds = 3600
    }
}
