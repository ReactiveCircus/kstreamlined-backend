package io.github.reactivecircus.kstreamlined.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KSBackendApplication

fun main(args: Array<String>) {
    runApplication<KSBackendApplication>(args = args)
}
