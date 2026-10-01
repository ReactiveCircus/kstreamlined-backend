package io.github.reactivecircus.kstreamlined.backend

import com.google.cloud.NoCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

class TestEnvironment(private val emulatorHost: String) : AutoCloseable {
    val services = ServiceHttpStubs()

    private val emulator = WebTestClient.bindToServer()
        .baseUrl("http://$emulatorHost")
        .responseTimeout(Duration.ofSeconds(1))
        .build()

    private val client = lazy {
        val options = FirestoreOptions.newBuilder()
            .setProjectId(ProjectId)
            .setHost(emulatorHost)
            .setEmulatorHost(emulatorHost)
            .setCredentials(NoCredentials.getInstance())
            .build()
        check(options.host == emulatorHost && options.emulatorHost == emulatorHost) {
            "Firestore client must target only the local emulator."
        }
        options.service
    }

    fun firestore(): Firestore = client.value

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
            if (client.isInitialized()) {
                client.value.close()
            }
        }
    }

    companion object {
        const val ProjectId = "demo-ks-integration"
    }
}
