package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.aot.KSRuntimeHints
import io.github.reactivecircus.kstreamlined.backend.aot.ThirdPartyRuntimeHints
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.context.annotation.ReflectiveScan

@SpringBootApplication
@ReflectiveScan
@ImportRuntimeHints(KSRuntimeHints::class, ThirdPartyRuntimeHints::class)
class KSBackendApplication

fun main(args: Array<String>) {
    runApplication<KSBackendApplication>(args = args)
}
