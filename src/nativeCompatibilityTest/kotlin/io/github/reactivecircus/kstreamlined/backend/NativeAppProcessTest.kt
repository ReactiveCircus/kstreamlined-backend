package io.github.reactivecircus.kstreamlined.backend

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import kotlin.system.exitProcess
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativeAppProcessTest {
    @TempDir
    lateinit var directory: Path

    @Test
    fun `early exit keeps the log and reports status and captured output`() = withEnvironment { environment ->
        val failure = assertFailsWith<IllegalStateException> {
            NativeAppProcess.start(executable("exit"), environment)
        }
        val message = checkNotNull(failure.message)
        assertTrue(message.contains("exit=17"), message)
        assertTrue(message.contains("Intentional startup failure"), message)
        assertStopped(message)
        Files.delete(logFile(message))
    }

    @Test
    fun `readiness timeout keeps the log and stops the launched process`() = withEnvironment { environment ->
        val failure = assertFailsWith<IllegalStateException> {
            NativeAppProcess.start(executable("timeout"), environment, Duration.ofSeconds(1))
        }
        val message = checkNotNull(failure.message)
        assertTrue(message.contains("timed out"), message)
        assertStopped(message)
        Files.delete(logFile(message))
    }

    @Test
    fun `graceful shutdown deletes the log and close is idempotent`() = withEnvironment { environment ->
        val application = NativeAppProcess.start(executable("ready"), environment)
        application.close()
        application.close()
        assertStopped(application.pid)
        assertFalse(Files.exists(application.logFile))
    }

    @Test
    fun `log is kept when requested`() = withEnvironment { environment ->
        val application = NativeAppProcess.start(executable("ready"), environment)
        application.close(keepLog = true)
        assertStopped(application.pid)
        Files.delete(application.logFile)
    }

    private fun withEnvironment(test: (TestEnvironment) -> Unit) {
        TestEnvironment("127.0.0.1:9", "demo-ks-process").use(test)
    }

    private fun executable(mode: String): Path {
        val java = Path.of(System.getProperty("java.home"), "bin", "java")
        val classpath = listOf(FakeNativeApp::class.java, Unit::class.java)
            .map { Path.of(it.protectionDomain.codeSource.location.toURI()).toString() }
            .distinct()
            .joinToString(File.pathSeparator)
        val executable = directory.resolve("application")
        Files.writeString(
            executable,
            $$"""
            |#!/bin/sh
            |exec "$$java" -cp "$$classpath" $${FakeNativeApp::class.java.name} $$mode "$@"
            |
            """.trimMargin(),
        )
        check(executable.toFile().setExecutable(true)) { "Cannot make process fixture executable." }
        return executable
    }

    private fun logFile(message: String): Path =
        Path.of(checkNotNull(Regex("Log: (\\S+)").find(message)).groupValues[1])

    private fun assertStopped(message: String) =
        assertStopped(checkNotNull(Regex("PID=(\\d+)").find(message)).groupValues[1].toLong())

    private fun assertStopped(pid: Long) {
        assertFalse(
            checkNotNull(ProcessHandle.of(pid).map { it.isAlive }.orElse(false)),
            "Process $pid is still running.",
        )
    }
}

object FakeNativeApp {
    @JvmStatic
    fun main(args: Array<String>) {
        check("-XX:MissingRegistrationReportingMode=Exit" in args)
        check(System.getenv("KS_GCLOUD_PROJECT_ID") == "demo-ks-process")
        when (args.first()) {
            "exit" -> {
                println("Intentional startup failure")
                exitProcess(17)
            }

            "timeout" -> Thread.sleep(60_000)

            else -> {
                val port = args.single { it.startsWith("--server.port=") }.substringAfter('=').toInt()
                val server = HttpServer.create(InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 0)
                server.createContext("/graphql") { exchange ->
                    exchange.requestBody.use { it.readAllBytes() }
                    val body = """{"data":{"__typename":"Query"}}""".toByteArray()
                    exchange.sendResponseHeaders(200, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                }
                server.start()
                CountDownLatch(1).await()
            }
        }
    }
}
