package io.github.reactivecircus.kstreamlined.backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class KSBackendApplication

fun main(args: Array<String>) {
    runApplication<KSBackendApplication>(args = args)
}
