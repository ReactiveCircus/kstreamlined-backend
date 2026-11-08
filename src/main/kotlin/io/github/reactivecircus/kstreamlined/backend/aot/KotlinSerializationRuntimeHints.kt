package io.github.reactivecircus.kstreamlined.backend.aot

import kotlinx.serialization.Serializable
import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.ReflectionHints
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.lang.reflect.Modifier

/**
 * Registers native image hints for the `serializer()` of `@Serializable` classes, which kotlinx.serialization looks up
 * reflectively at runtime.
 */
class KotlinSerializationRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        findClasses(classLoader, AnnotationTypeFilter(Serializable::class.java)).forEach { type ->
            hints.reflection().registerKotlinSerializer(type)
        }
    }

    private fun ReflectionHints.registerKotlinSerializer(type: Class<*>) {
        val companion = type.declaredFields.firstOrNull {
            it.name == "Companion" && Modifier.isStatic(it.modifiers)
        } ?: return
        registerField(companion)
        companion.type.declaredMethods
            .filter { it.name == "serializer" }
            .forEach { registerMethod(it, ExecutableMode.INVOKE) }
    }
}
