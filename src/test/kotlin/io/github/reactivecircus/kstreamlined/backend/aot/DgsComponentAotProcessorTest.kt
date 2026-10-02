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
    @Test
    fun `registers component and factory-produced component methods for invocation`() {
        AnnotationConfigApplicationContext().use { context ->
            context.register(FeedSourceDataFetcher::class.java, DgsFactoryMethodConfig::class.java)
            context.refreshForAotProcessing(RuntimeHints())
            val generationContext = DefaultGenerationContext(
                ClassNameGenerator(ClassName.get(javaClass.packageName, "Test")),
                InMemoryGeneratedFiles(),
            )
            context.beanFactory.beanDefinitionNames.forEach { name ->
                DgsComponentAotProcessor().processAheadOfTime(RegisteredBean.of(context.beanFactory, name))
                    ?.applyTo(generationContext, UnusedBeanRegistrationCode)
            }
            val hints = generationContext.runtimeHints
            val reflection = RuntimeHintsPredicates.reflection()

            assertTrue(reflection.onMethodInvocation(FeedSourceDataFetcher::class.java, "feedSources").test(hints))
            val bridge = DgsSpringGraphQLAutoConfiguration.DgsTypeDefinitionConfigurerBridge::class.java
            val dgsMethods = bridge.declaredMethods.filter {
                it.isAnnotationPresent(DgsTypeDefinitionRegistry::class.java)
            }
            assertTrue(dgsMethods.isNotEmpty())
            dgsMethods.forEach {
                assertTrue(reflection.onMethodInvocation(it).test(hints), it.toString())
            }
            assertNull(hints.reflection().getTypeHint<DgsFactoryMethodConfig>())
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
