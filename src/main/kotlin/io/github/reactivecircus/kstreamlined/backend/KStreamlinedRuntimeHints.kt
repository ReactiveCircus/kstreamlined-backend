package io.github.reactivecircus.kstreamlined.backend

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

object KStreamlinedRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.reflection()
            .registerType(
                TypeReference.of("com.github.benmanes.caffeine.cache.SSA")
            ) { builder ->
                builder.withConstructor(
                    listOf(
                        TypeReference.of("com.github.benmanes.caffeine.cache.Caffeine"),
                        TypeReference.of("com.github.benmanes.caffeine.cache.AsyncCacheLoader"),
                        TypeReference.of("boolean"),
                    ),
                    ExecutableMode.INVOKE,
                )
            }
    }
}
