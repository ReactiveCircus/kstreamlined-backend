package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/**
 * Registers native image hints for third-party libraries whose reflective access is not covered by
 * the GraalVM Reachability Metadata Repository, Spring AOT or metadata shipped with the libraries.
 */
class ThirdPartyRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        // Xerces (via skrape{it}) instantiates its parser configuration and DTD validator factory reflectively.
        listOf(
            "org.apache.xerces.parsers.XIncludeAwareParserConfiguration",
            "org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl",
        ).forEach { hints.registerNoArgConstructor(it) }
    }

    private fun RuntimeHints.registerNoArgConstructor(typeName: String) {
        reflection().registerType(TypeReference.of(typeName)) {
            it.withConstructor(emptyList(), ExecutableMode.INVOKE)
        }
    }
}
