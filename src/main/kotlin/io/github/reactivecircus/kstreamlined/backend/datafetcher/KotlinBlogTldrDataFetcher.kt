package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsMutation
import com.netflix.graphql.dgs.DgsQuery
import com.netflix.graphql.dgs.InputArgument
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.schema.generated.DgsConstants
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.BackfillKotlinBlogTldrsResult
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.service.KotlinBlogTldrService

@DgsComponent
class KotlinBlogTldrDataFetcher(
    private val service: KotlinBlogTldrService,
) {
    @DgsQuery(field = DgsConstants.QUERY.KotlinBlogTldr)
    suspend fun kotlinBlogTldr(@InputArgument id: String): KotlinBlogTldr? {
        return service.loadKotlinBlogTldr(id)?.toKotlinBlogTldr(id)
    }

    @DgsMutation(field = DgsConstants.MUTATION.GenerateKotlinBlogTldr)
    suspend fun generateKotlinBlogTldr(
        @InputArgument id: String,
        @InputArgument persist: Boolean,
    ): KotlinBlogTldr {
        return service.createKotlinBlogTldr(id, persist).toKotlinBlogTldr(id)
    }

    @DgsMutation(field = DgsConstants.MUTATION.BackfillKotlinBlogTldrs)
    suspend fun backfillKotlinBlogTldrs(): BackfillKotlinBlogTldrsResult {
        return service.backfillKotlinBlogTldrs().let {
            BackfillKotlinBlogTldrsResult(
                generatedCount = it.generatedCount,
                failedIds = it.failedIds,
            )
        }
    }
}
