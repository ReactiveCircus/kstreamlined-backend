package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.datasource.persister.KotlinBlogTldr

class FakeKotlinBlogTldrDataSource : KotlinBlogTldrDataSource {
    var nextKotlinBlogTldrResponse: suspend (String) -> KotlinBlogTldr = {
        throw KotlinBlogContentNotFoundException(it)
    }

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr {
        return nextKotlinBlogTldrResponse(id)
    }
}
