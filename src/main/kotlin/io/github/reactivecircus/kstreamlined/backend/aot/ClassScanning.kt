package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.type.filter.TypeFilter
import org.springframework.util.ClassUtils

internal const val BasePackage = "io.github.reactivecircus.kstreamlined.backend"

/**
 * Finds the top-level and static nested classes under [basePackage] matching [filter].
 */
internal fun findClasses(
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
