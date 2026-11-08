package io.github.reactivecircus.kstreamlined.backend.aot

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import kotlin.test.Test
import kotlin.test.assertTrue

class KotlinSerializationRuntimeHintsTest {
    private val hints = RuntimeHints().also {
        KotlinSerializationRuntimeHints().registerHints(it, javaClass.classLoader)
    }
    private val reflection = RuntimeHintsPredicates.reflection()

    @Test
    fun `registers Companion field access and serializer() invocation of @Serializable classes`() {
        val companion = CloudflareAiRequest::class.java.getDeclaredField("Companion")
        assertTrue(reflection.onFieldAccess(companion).test(hints))
        assertTrue(reflection.onMethodInvocation(companion.type, "serializer").test(hints))
    }
}
