package io.github.reactivecircus.kstreamlined.backend.aot

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsTypeDefinitionRegistry
import com.netflix.graphql.dgs.springgraphql.autoconfig.DgsSpringGraphQLAutoConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.FeedSourceDataFetcher
import org.springframework.aot.generate.ClassNameGenerator
import org.springframework.aot.generate.DefaultGenerationContext
import org.springframework.aot.generate.GeneratedMethods
import org.springframework.aot.generate.InMemoryGeneratedFiles
import org.springframework.aot.generate.MethodReference
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.getTypeHint
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates
import org.springframework.beans.factory.aot.BeanRegistrationCode
import org.springframework.beans.factory.support.RegisteredBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.javapoet.ClassName
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DgsComponentAotProcessorTest {
    private val hints = processAheadOfTime(FeedSourceDataFetcher::class.java, DgsFactoryMethodConfig::class.java)
    private val reflection = RuntimeHintsPredicates.reflection()

    @Test
    fun `registers methods of @DgsComponent classes for invocation`() {
        assertTrue(reflection.onMethodInvocation(FeedSourceDataFetcher::class.java, "feedSources").test(hints))
    }

    @Test
    fun `registers methods of DGS components returned from @Bean methods for invocation`() {
        val bridge = DgsSpringGraphQLAutoConfiguration.DgsTypeDefinitionConfigurerBridge::class.java
        val dgsMethods = bridge.declaredMethods.filter {
            it.isAnnotationPresent(DgsTypeDefinitionRegistry::class.java)
        }
        assertTrue(dgsMethods.isNotEmpty())
        dgsMethods.forEach {
            assertTrue(reflection.onMethodInvocation(it).test(hints), it.toString())
        }
    }

    @Test
    fun `does not register configuration classes declaring DGS component @Bean methods`() {
        assertNull(hints.reflection().getTypeHint<DgsFactoryMethodConfig>())
    }

    private fun processAheadOfTime(vararg componentClasses: Class<*>): RuntimeHints {
        return AnnotationConfigApplicationContext().use { context ->
            context.register(*componentClasses)
            context.refreshForAotProcessing(RuntimeHints())
            val generationContext = DefaultGenerationContext(
                ClassNameGenerator(ClassName.get(javaClass.packageName, "Test")),
                InMemoryGeneratedFiles(),
            )
            context.beanFactory.beanDefinitionNames.forEach { name ->
                DgsComponentAotProcessor().processAheadOfTime(RegisteredBean.of(context.beanFactory, name))
                    ?.applyTo(generationContext, UnusedBeanRegistrationCode)
            }
            generationContext.runtimeHints
        }
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
}
