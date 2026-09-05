package io.github.reactivecircus.kstreamlined.backend.datasource.persister

class FakeKotlinBlogTldrPersister : KotlinBlogTldrPersister {
    private val kotlinBlogTldrs = mutableMapOf<String, KotlinBlogTldr>()

    val savedKotlinBlogTldrs: Map<String, KotlinBlogTldr>
        get() = kotlinBlogTldrs.toMap()

    override fun loadKotlinBlogTldr(id: String): KotlinBlogTldr? {
        return kotlinBlogTldrs[id.firestoreDocumentId]
    }

    override fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldr) {
        kotlinBlogTldrs[id.firestoreDocumentId] = tldr
    }
}
