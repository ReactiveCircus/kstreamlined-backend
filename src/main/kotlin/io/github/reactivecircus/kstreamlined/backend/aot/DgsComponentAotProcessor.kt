package io.github.reactivecircus.kstreamlined.backend.aot

import com.netflix.graphql.dgs.DgsComponent
import org.springframework.aot.hint.MemberCategory
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor
import org.springframework.beans.factory.support.RegisteredBean

/**
 * DGS discovers `@DgsComponent` beans and invokes their annotated
 * methods (e.g. `@DgsQuery`, `@DgsData`, `@DgsTypeDefinitionRegistry`) reflectively.
 */
class DgsComponentAotProcessor : BeanRegistrationAotProcessor {
    override fun processAheadOfTime(registeredBean: RegisteredBean): BeanRegistrationAotContribution? {
        registeredBean.beanFactory.findAnnotationOnBean(
            registeredBean.beanName,
            DgsComponent::class.java,
            false,
        ) ?: return null
        val beanClass = registeredBean.beanType.toClass()
        return BeanRegistrationAotContribution { generationContext, _ ->
            generationContext.runtimeHints.reflection().registerType(
                beanClass,
                MemberCategory.INVOKE_DECLARED_METHODS,
                MemberCategory.INVOKE_PUBLIC_METHODS,
            )
        }
    }
}
