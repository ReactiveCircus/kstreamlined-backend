package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/**
 * Registers native image hints for third-party libraries whose dynamic access is not covered by
 * the GraalVM Reachability Metadata Repository, Spring AOT or metadata shipped with the libraries.
 */
class ThirdPartyRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        registerXercesNoArgConstructors(hints)
    }

    /**
     * Registers Xerces (via skrape{it}) parser configuration and DTD validator factory for reflection.
     */
    private fun registerXercesNoArgConstructors(hints: RuntimeHints) {
        listOf(
            "org.apache.xerces.parsers.XIncludeAwareParserConfiguration",
            "org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl",
        ).forEach { className ->
            hints.reflection().registerType(TypeReference.of(className)) {
                it.withConstructor(emptyList(), ExecutableMode.INVOKE)
            }
        }
    }
}
