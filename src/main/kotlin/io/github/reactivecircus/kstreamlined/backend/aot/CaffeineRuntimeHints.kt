package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

/**
 * Registers native image hints for Caffeine classes missing from the GraalVM reachability metadata.
 *
 * Caffeine instantiates node classes reflectively based on the cache's features. The reachability metadata doesn't
 * cover the node class of size-bounded caches without stats (e.g. the APQ cache).
 */
class CaffeineRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.reflection().registerTypeIfPresent(
            classLoader,
            "com.github.benmanes.caffeine.cache.PSMS",
            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
        )
    }
}
