package io.github.reactivecircus.kstreamlined.backend.datasource.persister

class FakeKotlinBlogTldrPersister : KotlinBlogTldrPersister {
    private val kotlinBlogTldrs = mutableMapOf<String, KotlinBlogTldrSummary>()

    val savedKotlinBlogTldrs: Map<String, KotlinBlogTldrSummary>
        get() = kotlinBlogTldrs.toMap()

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
        return kotlinBlogTldrs[id.firestoreDocumentId]
    }

    override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
        kotlinBlogTldrs[id.firestoreDocumentId] = tldr
    }
}
