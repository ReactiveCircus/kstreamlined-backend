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
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import kotlin.time.Duration

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
            requestTimeoutMillis = 5_000L
            socketTimeoutMillis = 5_000L
        }
    }

    /**
     * Returns the value of [key], or null if the key doesn't exist. Error responses and timeouts are logged and also
     * return null.
     */
    suspend fun get(key: String): String? {
        return executeWithErrorHandling(operation = "GET", key = key) {
            httpClient.get("$url/get/$key") {
                bearerAuth(token)
            }.bodyAsText().let { result ->
                DefaultJson.decodeFromString<Map<String, String?>>(result)["result"]
            }
        }
    }

    /**
     * Sets [key] to [value] with the given [expiry]. If [value] is unchanged (its checksum matches the one kept in
     * `<key>:checksum`), only the expiry is refreshed and the value isn't sent again. Error responses and timeouts are
     * logged and ignored.
     */
    suspend fun setIfChanged(key: String, value: String, expiry: Duration) {
        val checksumKey = "$key$ChecksumKeySuffix"
        val checksum = value.sha256()
        val expirySeconds = expiry.inWholeSeconds.toString()
        val unchanged = executeWithErrorHandling(operation = "EVAL", key = key) {
            val command = buildJsonArray {
                add("EVAL")
                add(RefreshExpiryIfUnchangedScript)
                add("2")
                add(key)
                add(checksumKey)
                add(checksum)
                add(expirySeconds)
            }
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                setBody(command.toString())
                bearerAuth(token)
            }.bodyAsText()
            val result = (DefaultJson.parseToJsonElement(response) as? JsonObject)?.get("result")
            (result as? JsonPrimitive)?.intOrNull == 1
        }
        if (unchanged == true) {
            logger.info("Remote cache value for key: $key is unchanged, refreshed expiry.")
            return
        }

        // write value and checksum atomically so they can't get out of sync
        val written = executeWithErrorHandling(operation = "MULTI-EXEC", key = key) {
            val commands = buildJsonArray {
                addJsonArray {
                    add("SET")
                    add(key)
                    add(value)
                    add("EX")
                    add(expirySeconds)
                }
                addJsonArray {
                    add("SET")
                    add(checksumKey)
                    add(checksum)
                    add("EX")
                    add(expirySeconds)
                }
            }
            val response = httpClient.post("$url/multi-exec") {
                contentType(ContentType.Application.Json)
                setBody(commands.toString())
                bearerAuth(token)
            }.bodyAsText()
            val results = DefaultJson.parseToJsonElement(response) as? JsonArray
            results?.size == 2 && results.all {
                ((it as? JsonObject)?.get("result") as? JsonPrimitive)?.contentOrNull == "OK"
            }.also { success ->
                if (!success) logger.error("Unexpected response during Redis MULTI-EXEC for key: $key: $response")
            }
        }
        if (written == true) {
            logger.info("Remote cache value for key: $key has changed, wrote ${value.length} chars.")
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
        } catch (e: SerializationException) {
            logger.error("Malformed response during Redis $operation operation for key: $key", e)
            null
        }
    }

    companion object {
        private const val ChecksumKeySuffix = ":checksum"

        // Returns 1 and refreshes the expiry of both keys if the stored checksum (KEYS[2]) matches ARGV[1]
        // and the value (KEYS[1]) still exists, otherwise returns 0.
        private const val RefreshExpiryIfUnchangedScript =
            "if redis.call('GET', KEYS[2]) == ARGV[1] and redis.call('EXPIRE', KEYS[1], ARGV[2]) == 1 then " +
                "redis.call('EXPIRE', KEYS[2], ARGV[2]) return 1 end return 0"
    }
}

private fun String.sha256() = MessageDigest.getInstance("SHA-256").digest(encodeToByteArray()).toHexString()
