package io.github.reactivecircus.kstreamlined.backend

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import org.testcontainers.gcloud.FirestoreEmulatorContainer
import org.testcontainers.utility.DockerImageName

fun firestoreEmulator(): FirestoreEmulatorContainer =
    FirestoreEmulatorContainer(DockerImageName.parse(FirestoreEmulatorImage))
        .withCreateContainerCmdModifier { command ->
            checkNotNull(command.hostConfig).withPortBindings(
                PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 0), ExposedPort.tcp(8080)),
            )
        }

private const val FirestoreEmulatorImage = "gcr.io/google.com/cloudsdktool/google-cloud-cli:587.0.0-emulators"
