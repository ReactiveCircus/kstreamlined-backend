package io.github.reactivecircus.kstreamlined.backend

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsTypeDefinitionRegistry
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import io.github.reactivecircus.kstreamlined.backend.aot.DgsComponentAotProcessor
import io.github.reactivecircus.kstreamlined.backend.aot.KSRuntimeHints
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest
import io.github.reactivecircus.kstreamlined.backend.datafetcher.FeedSourceDataFetcher
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.store.FirestoreModel
import org.springframework.aot.generate.ClassNameGenerator
import org.springframework.aot.generate.DefaultGenerationContext
import org.springframework.aot.generate.GeneratedMethods
import org.springframework.aot.generate.InMemoryGeneratedFiles
import org.springframework.aot.generate.MethodReference
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.annotation.ReflectiveRuntimeHintsRegistrar
import org.springframework.aot.hint.getTypeHint
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.beans.factory.aot.BeanRegistrationCode
import org.springframework.beans.factory.support.RegisteredBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.javapoet.ClassName
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KSRuntimeHintsTest {
    private val reflection = RuntimeHintsPredicates.reflection()

    @Test
    fun `Firestore models are annotated and registered for reflection`() {
        val models = findFirestoreModels().flatMap { it.withNestedAppTypes() }.toSet()
        assertTrue(models.isNotEmpty())

        val hints = RuntimeHints()
        ReflectiveRuntimeHintsRegistrar().registerRuntimeHints(hints, *models.toTypedArray())

        models.forEach { type ->
            assertTrue(type.isAnnotationPresent(FirestoreModel::class.java))
            assertTrue(reflection.onConstructorInvocation(type.getDeclaredConstructor()).test(hints))
            type.getters().forEach {
                assertTrue(reflection.onMethodInvocation(it).test(hints))
            }
            type.instanceFields().forEach {
                assertTrue(reflection.onFieldAccess(it).test(hints))
            }
        }
    }

    @Test
    fun `KSRuntimeHints discovers generated schema types, schema files and serializers`() {
        val hints = RuntimeHints().also { KSRuntimeHints().registerHints(it, javaClass.classLoader) }

        assertTrue(reflection.onMethodInvocation(KotlinBlogTldr::class.java, "getContent").test(hints))
        assertTrue(RuntimeHintsPredicates.resource().forResource("schema/kstreamlined.graphqls").test(hints))
        val companion = CloudflareAiRequest::class.java.getDeclaredField("Companion")
        assertTrue(reflection.onFieldAccess(companion).test(hints))
        assertTrue(reflection.onMethodInvocation(companion.type, "serializer").test(hints))
    }

    @Test
    fun `DgsComponentAotProcessor registers methods of DGS components for invocation`() {
        val context = AnnotationConfigApplicationContext().apply {
            register(FeedSourceDataFetcher::class.java, DgsFactoryMethodConfig::class.java)
            refreshForAotProcessing(RuntimeHints())
        }
        val generationContext = DefaultGenerationContext(
            ClassNameGenerator(ClassName.get(javaClass.packageName, "Test")),
            InMemoryGeneratedFiles(),
        )
        context.beanFactory.beanDefinitionNames.forEach { name ->
            DgsComponentAotProcessor().processAheadOfTime(RegisteredBean.of(context.beanFactory, name))
                ?.applyTo(generationContext, UnusedBeanRegistrationCode)
        }
        val hints = generationContext.runtimeHints

        assertTrue(reflection.onMethodInvocation(FeedSourceDataFetcher::class.java, "feedSources").test(hints))
        val bridge = DgsSpringGraphQLAutoConfiguration.DgsTypeDefinitionConfigurerBridge::class.java
        val dgsMethods = bridge.declaredMethods.filter { it.isAnnotationPresent(DgsTypeDefinitionRegistry::class.java) }
        assertTrue(dgsMethods.isNotEmpty())
        dgsMethods.forEach {
            assertTrue(reflection.onMethodInvocation(it).test(hints))
        }
        assertNull(hints.reflection().getTypeHint<DgsFactoryMethodConfig>())
    }

    @Configuration(proxyBeanMethods = false)
    class DgsFactoryMethodConfig {
        @Bean
        @DgsComponent
        fun dgsTypeDefinitionConfigurerBridge() = DgsSpringGraphQLAutoConfiguration.DgsTypeDefinitionConfigurerBridge()
    }

    private object UnusedBeanRegistrationCode : BeanRegistrationCode {
        override fun getClassName(): ClassName = error("Not used by DgsComponentAotProcessor")
        override fun getMethods(): GeneratedMethods = error("Not used by DgsComponentAotProcessor")
        override fun addInstancePostProcessor(methodReference: MethodReference) =
            error("Not used by DgsComponentAotProcessor")
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
