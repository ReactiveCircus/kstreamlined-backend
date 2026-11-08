package io.github.reactivecircus.kstreamlined.backend.aot

import org.springframework.aot.hint.MemberCategory
import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar

/**
 * Registers native image hints for the GraphQL schema: the schema files loaded from the classpath, and the DGS codegen
 * types whose getters are invoked reflectively when resolving fields.
 */
class GraphQLSchemaRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        hints.resources().registerPattern("schema").registerPattern("schema/**/*.graphql*")

        findClasses(classLoader, { _, _ -> true }, basePackage = GeneratedSchemaTypesPackage).forEach { type ->
            hints.reflection().registerType(type, MemberCategory.INVOKE_PUBLIC_METHODS)
        }
    }

    private companion object {
        const val GeneratedSchemaTypesPackage = "$BasePackage.schema.generated.types"
    }
}
