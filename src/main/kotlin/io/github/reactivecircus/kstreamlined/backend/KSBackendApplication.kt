package io.github.reactivecircus.kstreamlined.backend

import io.github.reactivecircus.kstreamlined.backend.aot.GraphQLSchemaRuntimeHints
import io.github.reactivecircus.kstreamlined.backend.aot.KotlinSerializationRuntimeHints
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ImportRuntimeHints
import org.springframework.context.annotation.ReflectiveScan

@SpringBootApplication
@ReflectiveScan
@ImportRuntimeHints(GraphQLSchemaRuntimeHints::class, KotlinSerializationRuntimeHints::class)
class KSBackendApplication

fun main(args: Array<String>) {
    runApplication<KSBackendApplication>(args = args)
}
