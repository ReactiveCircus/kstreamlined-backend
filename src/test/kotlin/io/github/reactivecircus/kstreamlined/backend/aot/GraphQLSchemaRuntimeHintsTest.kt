package io.github.reactivecircus.kstreamlined.backend.aot

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import kotlin.test.Test
import kotlin.test.assertTrue

class GraphQLSchemaRuntimeHintsTest {
    private val hints = RuntimeHints().also { GraphQLSchemaRuntimeHints().registerHints(it, javaClass.classLoader) }

    @Test
    fun `registers GraphQL schema files for resource loading`() {
        assertTrue(RuntimeHintsPredicates.resource().forResource("schema/kstreamlined.graphqls").test(hints))
    }

    @Test
    fun `registers public methods of DGS codegen types for invocation`() {
        assertTrue(
            RuntimeHintsPredicates.reflection().onMethodInvocation(KotlinBlogTldr::class.java, "getContent").test(hints),
        )
    }
}
