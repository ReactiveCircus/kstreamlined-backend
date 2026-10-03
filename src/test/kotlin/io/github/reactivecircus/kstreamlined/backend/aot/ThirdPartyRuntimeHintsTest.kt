package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ThirdPartyRuntimeHintsTest {
    @Test
    fun `registers Xerces no-argument constructors for invocation`() {
        val hints = RuntimeHints().also { ThirdPartyRuntimeHints().registerHints(it, javaClass.classLoader) }
        val reflection = RuntimeHintsPredicates.reflection()
        listOf(
            "org.apache.xerces.parsers.XIncludeAwareParserConfiguration",
            "org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl",
        ).forEach { name ->
            val type = Class.forName(name)
            assertTrue(reflection.onConstructorInvocation(type.getDeclaredConstructor()).test(hints), name)
            val hint = assertNotNull(hints.reflection().getTypeHint(type), name)
            assertEquals(1L, hint.constructors().count(), name)
            assertTrue(hint.memberCategories.isEmpty(), name)
            assertTrue(hint.methods().findAny().isEmpty, name)
        }
    }
}
