package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem

class FakeKotlinBlogContentPersister : KotlinBlogContentPersister {
    private val kotlinBlogContents = mutableMapOf<String, KotlinBlogContent>()

    val savedKotlinBlogContents: Map<String, KotlinBlogContent>
        get() = kotlinBlogContents.toMap()

    override fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
        return kotlinBlogContents[id.firestoreDocumentId]
    }

    override fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>) {
        items.forEach { item ->
            kotlinBlogContents.putIfAbsent(
                item.firestoreDocumentId,
                KotlinBlogContent.from(item),
            )
        }
    }
}
