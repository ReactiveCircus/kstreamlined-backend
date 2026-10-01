package io.github.reactivecircus.kstreamlined.backend.integration

import com.google.cloud.NoCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

object IntegrationEnvironment : AutoCloseable {
    const val ProjectId = "demo-ks-integration"

    private val emulatorHost = requireEmulatorHost(System.getenv("FIRESTORE_EMULATOR_HOST"))

    val services = ServiceHttpStubs()

    private val clients = CopyOnWriteArrayList<Firestore>()
    private val emulator = WebTestClient.bindToServer()
        .baseUrl("http://$emulatorHost")
        .responseTimeout(Duration.ofSeconds(1))
        .build()

    fun firestore(): Firestore {
        val options = FirestoreOptions.newBuilder()
            .setProjectId(ProjectId)
            .setHost(emulatorHost)
            .setEmulatorHost(emulatorHost)
            .setCredentials(NoCredentials.getInstance())
            .build()
        check(options.host == emulatorHost && options.emulatorHost == emulatorHost) {
            "Firestore client must target only the local emulator."
        }

        return options.service.also { clients.add(it) }
    }

    fun reset() {
        emulator.delete()
            .uri("/emulator/v1/projects/$ProjectId/databases/(default)/documents")
            .exchange()
            .expectStatus().isOk
        services.reset()
    }

    override fun close() {
        try {
            services.close()
        } finally {
            clients.forEach { it.close() }
        }
    }
}

internal fun requireEmulatorHost(value: String?): String {
    val host = requireNotNull(value) {
        "Start a local Firestore emulator and set FIRESTORE_EMULATOR_HOST=127.0.0.1:<port>."
    }
    val match = requireNotNull(Regex("""(127\.0\.0\.1|localhost):(\d+)""").matchEntire(host)) {
        "Integration tests require a loopback FIRESTORE_EMULATOR_HOST, not '$host'."
    }
    val port = match.groupValues[2].toIntOrNull()
    require(port != null && port in 1..65535) { "Invalid Firestore emulator port: $host." }
    return host
}
