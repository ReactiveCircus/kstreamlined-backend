package io.github.reactivecircus.kstreamlined.backend.aot

import io.github.reactivecircus.kstreamlined.backend.store.FirestoreModel
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import kotlin.test.Test
import kotlin.test.assertTrue

class FirestoreModelHintsTest {
    @Test
    fun `Firestore models and nested types are annotated and registered for reflection`() {
        val models = findFirestoreModels().flatMap { it.withNestedAppTypes() }.toSet()
        assertTrue(models.isNotEmpty())

        val hints = RuntimeHints()
        ReflectiveRuntimeHintsRegistrar().registerRuntimeHints(hints, *models.toTypedArray())
        val reflection = RuntimeHintsPredicates.reflection()

        models.forEach { type ->
            assertTrue(type.isAnnotationPresent(FirestoreModel::class.java), type.name)
            assertTrue(reflection.onConstructorInvocation(type.getDeclaredConstructor()).test(hints), type.name)
            type.getters().forEach {
                assertTrue(reflection.onMethodInvocation(it).test(hints), it.toString())
            }
            type.instanceFields().forEach {
                assertTrue(reflection.onFieldAccess(it).test(hints), it.toString())
            }
        }
    }

    private fun findFirestoreModels(): List<Class<*>> {
        val scanner = object : ClassPathScanningCandidateComponentProvider(false) {
            override fun isCandidateComponent(beanDefinition: AnnotatedBeanDefinition) =
                beanDefinition.metadata.isIndependent
        }
        scanner.addIncludeFilter(AnnotationTypeFilter(FirestoreModel::class.java))
        return scanner.findCandidateComponents(AppPackage).map { Class.forName(it.beanClassName) }
    }

    private fun Class<*>.withNestedAppTypes(visited: MutableSet<Class<*>> = mutableSetOf()): Set<Class<*>> {
        if (!visited.add(this)) return visited
        instanceFields().forEach { field ->
            val genericType = field.genericType
            val typeArguments = (genericType as? ParameterizedType)?.actualTypeArguments.orEmpty()
            (typeArguments.filterIsInstance<Class<*>>() + field.type)
                .filter { it.name.startsWith("$AppPackage.") }
                .forEach { it.withNestedAppTypes(visited) }
        }
        return visited
    }

    private fun Class<*>.getters() = declaredMethods.filter {
        Modifier.isPublic(it.modifiers) && !Modifier.isStatic(it.modifiers) && it.parameterCount == 0 &&
            (it.name.startsWith("get") || it.name.startsWith("is"))
    }

    private fun Class<*>.instanceFields() = declaredFields.filter { !Modifier.isStatic(it.modifiers) }

    private companion object {
        const val AppPackage = "io.github.reactivecircus.kstreamlined.backend"
    }
}
