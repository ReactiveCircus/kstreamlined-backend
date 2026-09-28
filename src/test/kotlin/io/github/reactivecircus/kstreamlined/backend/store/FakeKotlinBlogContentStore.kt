package io.github.reactivecircus.kstreamlined.backend.store

import io.github.reactivecircus.kstreamlined.backend.service.dto.KotlinBlogItem

class FakeKotlinBlogContentStore : KotlinBlogContentStore {
    val allKotlinBlogContents: Map<String, KotlinBlogContent>
        field = mutableMapOf<String, KotlinBlogContent>()

    val allKotlinBlogTldrs: Map<String, KotlinBlogContent.Tldr>
        get() = allKotlinBlogContents.values.mapNotNull { content ->
            content.tldr?.let { tldr -> content.id.firestoreDocumentId to tldr }
        }.toMap()

    override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
        return allKotlinBlogContents[id.firestoreDocumentId]
    }

    override suspend fun loadKotlinBlogContentsWithoutTldr(): List<KotlinBlogContent> {
        return allKotlinBlogContents.values.filter { it.tldr == null }
    }

    override suspend fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>) {
        items.forEach { item ->
            allKotlinBlogContents.putIfAbsent(
                item.firestoreDocumentId,
                KotlinBlogContent.from(item),
            )
        }
    }

    override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
        tldrs.forEach { (id, tldr) ->
            allKotlinBlogContents[id.firestoreDocumentId]?.let { content ->
                allKotlinBlogContents[id.firestoreDocumentId] = content.copy(tldr = tldr)
            }
        }
    }
}
