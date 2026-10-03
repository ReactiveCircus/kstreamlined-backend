package io.github.reactivecircus.kstreamlined.backend

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration

class TestEnvironment(
    val emulatorHost: String,
    val projectId: String,
) : AutoCloseable {
    val services = ServiceHttpStubs()

    private val emulator = WebTestClient.bindToServer()
        .baseUrl("http://$emulatorHost")
        .responseTimeout(Duration.ofSeconds(1))
        .build()

    private val client = lazy {
        val options = FirestoreOptions.newBuilder()
            .setProjectId(projectId)
            .setHost(emulatorHost)
            .setEmulatorHost(emulatorHost)
            .setCredentials(FirestoreOptions.EmulatorCredentials())
            .build()
        options.service
    }

    val firestore: Firestore = client.value

    fun reset() {
        resetFirestore()
        services.reset()
    }

    fun resetFirestore() {
        emulator.delete()
            .uri("/emulator/v1/projects/$projectId/databases/(default)/documents")
            .exchange()
            .expectStatus().isOk
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
}
