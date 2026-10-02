package io.github.reactivecircus.kstreamlined.backend.aot

import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import kotlin.test.Test
import kotlin.test.assertTrue

class KSRuntimeHintsTest {
    private val hints = RuntimeHints().also { KSRuntimeHints().registerHints(it, javaClass.classLoader) }
    private val reflection = RuntimeHintsPredicates.reflection()

    @Test
    fun `registers generated schema methods for invocation`() {
        assertTrue(reflection.onMethodInvocation(KotlinBlogTldr::class.java, "getContent").test(hints))
    }

    @Test
    fun `registers GraphQL schema resources`() {
        assertTrue(RuntimeHintsPredicates.resource().forResource("schema/kstreamlined.graphqls").test(hints))
    }

    @Test
    fun `registers Kotlin serializer lookup and invocation`() {
        val companion = CloudflareAiRequest::class.java.getDeclaredField("Companion")
        assertTrue(reflection.onFieldAccess(companion).test(hints))
        assertTrue(reflection.onMethodInvocation(companion.type, "serializer").test(hints))
    }
}
