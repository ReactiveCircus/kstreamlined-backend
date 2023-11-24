package io.github.reactivecircus.kstreamlined.backend.scalar

import com.netflix.graphql.dgs.DgsScalar
import graphql.GraphQLContext
import graphql.execution.CoercedVariables
import graphql.language.StringValue
import graphql.language.Value
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * This assumes the input is already an ISO-8601 string.
 */
@DgsScalar(name = "Instant")
class InstantScalar : Coercing<Instant, String> {
    override fun serialize(dataFetcherResult: Any, graphQLContext: GraphQLContext, locale: Locale): String? {
        if (dataFetcherResult !is Instant) throw CoercingParseLiteralException("Result is not an Instant.")
        return DateTimeFormatter.ISO_INSTANT.format(dataFetcherResult)
    }

    override fun parseValue(input: Any, graphQLContext: GraphQLContext, locale: Locale): Instant? {
        return Instant.parse(input.toString())
    }

    override fun parseLiteral(
        input: Value<*>,
        variables: CoercedVariables,
        graphQLContext: GraphQLContext,
        locale: Locale
    ): Instant? {
        if (input !is StringValue) throw CoercingParseLiteralException("Input value is not a String.")
        return Instant.parse(input.value)
    }
}
