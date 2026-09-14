package io.github.reactivecircus.kstreamlined.backend.datasource.persister

class FakeKotlinBlogTldrPersister : KotlinBlogTldrPersister {
    private val kotlinBlogTldrs = mutableMapOf<String, KotlinBlogTldr>()

    val savedKotlinBlogTldrs: Map<String, KotlinBlogTldr>
        get() = kotlinBlogTldrs.toMap()

    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr? {
        return kotlinBlogTldrs[id.firestoreDocumentId]
    }

    override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldr) {
        kotlinBlogTldrs[id.firestoreDocumentId] = tldr
    }
}
