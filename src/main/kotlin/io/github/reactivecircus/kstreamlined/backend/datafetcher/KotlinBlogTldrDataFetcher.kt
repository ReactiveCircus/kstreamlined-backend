package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinBlogTldrDataSource
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr

@DgsComponent
class KotlinBlogTldrDataFetcher(
    private val dataSource: KotlinBlogTldrDataSource,
) {
    @DgsQuery(field = DgsConstants.QUERY.KotlinBlogTldr)
    suspend fun kotlinBlogTldr(@InputArgument id: String): KotlinBlogTldr? {
        return dataSource.loadKotlinBlogTldr(id)?.toKotlinBlogTldr(id)
    }

    @DgsMutation(field = DgsConstants.MUTATION.GenerateKotlinBlogTldr)
    suspend fun generateKotlinBlogTldr(
        @InputArgument id: String,
        @InputArgument persist: Boolean,
    ): KotlinBlogTldr {
        return dataSource.createKotlinBlogTldr(id, persist).toKotlinBlogTldr(id)
    }
}
