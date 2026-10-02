package io.github.reactivecircus.kstreamlined.backend

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.ConnectException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.io.path.readText

class NativeAppProcess private constructor(
    private val process: Process,
    val baseUrl: String,
    val logFile: Path,
) : AutoCloseable {
    val pid: Long get() = process.pid()

    private var closed = false
    private val shutdownHook = Thread({ stopProcess() }, "native-test-shutdown-${process.pid()}")

    override fun close() = close(keepLog = false)

    /**
     * Stops the application and verifies a graceful shutdown. The log is deleted unless [keepLog] is set
     * or verification fails.
     */
    fun close(keepLog: Boolean) {
        if (closed) return
        closed = true
        val wasRunning = process.isAlive
        val graceful = try {
            stopProcess()
        } finally {
            Runtime.getRuntime().removeShutdownHook(shutdownHook)
        }
        check(wasRunning) { diagnostics("Native process exited unexpectedly.") }
        check(graceful) { diagnostics("Native process required forced termination.") }
        check(process.exitValue() == 0 || process.exitValue() == 143) {
            diagnostics("Native process exited abnormally during shutdown.")
        }
        if (!keepLog) Files.deleteIfExists(logFile)
    }

    private fun stopProcess(): Boolean {
        if (!process.isAlive) return true
        process.destroy()
        return try {
            val graceful = process.waitFor(10, TimeUnit.SECONDS)
            if (!graceful) {
                process.destroyForcibly()
                check(process.waitFor(10, TimeUnit.SECONDS)) { diagnostics("Native process could not be stopped.") }
            }
            graceful
        } catch (failure: InterruptedException) {
            process.destroyForcibly()
            throw failure
        }
    }

    private fun awaitReady(timeout: Duration) {
        val deadline = System.nanoTime() + timeout.toNanos()
        var lastFailure: Exception? = null
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build().use { client ->
            val request = HttpRequest.newBuilder(URI.create("$baseUrl/graphql"))
                .timeout(Duration.ofSeconds(1))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("""{"query":"{ __typename }"}"""))
                .build()
            while (System.nanoTime() < deadline) {
                check(process.isAlive) { diagnostics("Native process exited before readiness.") }
                val response = try {
                    client.send(request, HttpResponse.BodyHandlers.ofString())
                } catch (failure: ConnectException) {
                    lastFailure = failure
                    null
                } catch (failure: HttpTimeoutException) {
                    lastFailure = failure
                    null
                }
                if (response != null) {
                    check(response.statusCode() == 200) {
                        diagnostics("Readiness HTTP ${response.statusCode()}: ${response.body()}")
                    }
                    val body = Json.parseToJsonElement(response.body()).jsonObject
                    val typeName = body["data"]?.jsonObject?.get("__typename")?.jsonPrimitive?.content
                    check("errors" !in body && typeName == "Query") {
                        diagnostics("Invalid GraphQL readiness response: ${response.body()}")
                    }
                    return
                }
                Thread.sleep(100)
            }
        }
        throw IllegalStateException(diagnostics("Native readiness timed out after $timeout."), lastFailure)
    }

    fun diagnostics(message: String): String {
        val status = if (process.isAlive) "running" else "exit=${process.exitValue()}"
        return "$message PID=${process.pid()}, $status. Log: $logFile\n" +
            logFile.readText().lines().takeLast(80).joinToString("\n")
    }

    companion object {
        fun start(
            executable: Path,
            environment: TestEnvironment,
            startupTimeout: Duration = Duration.ofSeconds(10),
        ): NativeAppProcess {
            require(Files.isRegularFile(executable) && Files.isExecutable(executable)) {
                "Native executable does not exist or is not executable: $executable"
            }
            val log = Files.createTempFile("ks-native-", ".log")
            val port = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).use { it.localPort }
            val url = environment.services.baseUrl
            val builder = ProcessBuilder(
                executable.toAbsolutePath().toString(),
                "-XX:MissingRegistrationReportingMode=Exit",
                "--server.address=127.0.0.1",
                "--server.port=$port",
                "--ks.kotlin-blog-feed-url=$url/feeds/blog",
                "--ks.kotlin-youtube-feed-url=$url/feeds/youtube",
                "--ks.talking-kotlin-feed-url=$url/feeds/podcast",
                "--ks.kotlin-weekly-feed-url=$url/feeds/weekly",
            )
            val inherited = builder.environment().toMap()
            builder.environment().apply {
                clear()
                listOf("PATH", "TMPDIR").forEach { name ->
                    inherited[name]?.let { put(name, it) }
                }
                putAll(
                    mapOf(
                        "FIRESTORE_EMULATOR_HOST" to environment.emulatorHost,
                        "KS_GCLOUD_PROJECT_ID" to environment.projectId,
                        "KS_REDIS_REST_URL" to "$url/redis",
                        "KS_REDIS_REST_TOKEN" to "native-test-token",
                        "KS_CF_BASE_URL" to "$url/ai",
                        "KS_CF_ACCOUNT_ID" to "integration",
                        "KS_CF_API_TOKEN" to "native-test-token",
                    ),
                )
            }
            val process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start()
            val application = NativeAppProcess(process, "http://127.0.0.1:$port", log)
            return runCatching {
                Runtime.getRuntime().addShutdownHook(application.shutdownHook)
                application.awaitReady(startupTimeout)
                application
            }.onFailure { failure ->
                runCatching { application.close(keepLog = true) }.exceptionOrNull()?.let(failure::addSuppressed)
            }.getOrThrow()
        }
    }
}
