package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import kotlin.test.Test
import kotlin.test.assertTrue

class CaffeineRuntimeHintsTest {
    private val hints = RuntimeHints().also { CaffeineRuntimeHints().registerHints(it, javaClass.classLoader) }

    @Test
    fun `registers constructor of node class used by size-bounded caches without stats for invocation`() {
        val nodeClass = Class.forName("com.github.benmanes.caffeine.cache.PSMS")
        val constructor = nodeClass.getDeclaredConstructor()
        assertTrue(RuntimeHintsPredicates.reflection().onConstructorInvocation(constructor).test(hints))
    }
}
