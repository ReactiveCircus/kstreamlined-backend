package io.github.reactivecircus.kstreamlined.backend.aot

import kotlinx.serialization.Serializable
import org.springframework.aot.hint.ExecutableMode
import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.ReflectionHints
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.core.type.filter.TypeFilter
import org.springframework.util.ClassUtils
import java.lang.reflect.Modifier

/**
 * Registers native image hints for first-party classes.
 */
class KSRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.resources().registerPattern("schema").registerPattern("schema/**/*.graphql*")

        val reflection = hints.reflection()

        findClasses(classLoader, { _, _ -> true }, basePackage = GeneratedSchemaTypesPackage).forEach { type ->
            reflection.registerType(type, MemberCategory.INVOKE_PUBLIC_METHODS)
        }

        findClasses(classLoader, AnnotationTypeFilter(Serializable::class.java)).forEach { type ->
            reflection.registerKotlinSerializer(type)
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

    private fun findClasses(
        classLoader: ClassLoader?,
        filter: TypeFilter,
        basePackage: String = BasePackage,
    ): List<Class<*>> {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition): Boolean {
                return beanDefinition.metadata.isIndependent
            }
        }
        scanner.setResourceLoader(DefaultResourceLoader(classLoader))
        scanner.addIncludeFilter(filter)
        return scanner.findCandidateComponents(basePackage).map {
            ClassUtils.forName(checkNotNull(it.beanClassName), classLoader)
        }
    }

    private companion object {
        const val BasePackage = "io.github.reactivecircus.kstreamlined.backend"
        const val GeneratedSchemaTypesPackage = "$BasePackage.schema.generated.types"
    }
}
