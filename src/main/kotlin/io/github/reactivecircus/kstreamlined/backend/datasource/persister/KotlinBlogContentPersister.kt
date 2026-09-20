package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.api.core.ApiFutures
import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.NoArg
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import java.time.Instant

interface KotlinBlogContentPersister {
    suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent?

    suspend fun loadKotlinBlogContentsWithoutTldr(): List<KotlinBlogContent>

    suspend fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>)

    suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>)
}

@NoArg
data class KotlinBlogContent(
    val id: String,
    val title: String,
    val html: String,
    val tldr: Tldr?,
) {
    @NoArg
    data class Tldr(
        val output: String,
        val model: String,
        val generatedAt: Instant,
        val promptTokens: Int?,
        val completionTokens: Int?,
        val totalTokens: Int?,
        val neurons: Double?,
        val generationDurationMs: Long,
    )
    companion object {
        fun from(item: KotlinBlogItem): KotlinBlogContent {
            return KotlinBlogContent(
                id = item.guid,
                title = item.title,
                html = requireNotNull(item.html?.trim()),
                tldr = null,
            )
        }
    }
}

class FirestoreKotlinBlogContentPersister(
    private val firestore: Firestore,
) : KotlinBlogContentPersister {
    override suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent? {
        return firestore.collection(KotlinBlogContentCollectionPath)
            .document(id.firestoreDocumentId)
            .get()
            .await()
            .toObject(KotlinBlogContent::class.java)
    }

    override suspend fun loadKotlinBlogContentsWithoutTldr(): List<KotlinBlogContent> {
        return firestore.collection(KotlinBlogContentCollectionPath)
            .whereEqualTo("tldr", null)
            .get()
            .await()
            .toObjects(KotlinBlogContent::class.java)
    }

    override suspend fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>) {
        if (items.isEmpty()) return

        val docRefToContentMap = items.associateBy { item ->
            firestore.collection(KotlinBlogContentCollectionPath)
                .document(item.firestoreDocumentId)
        }
        val docRefs = docRefToContentMap.keys.toTypedArray()

        firestore.runAsyncTransaction { transaction ->
            ApiFutures.transform(
                transaction.getAll(*docRefs),
                { snapshots ->
                    for (snapshot in snapshots) {
                        if (!snapshot.exists()) {
                            val content = KotlinBlogContent.from(
                                item = checkNotNull(docRefToContentMap[snapshot.reference]),
                            )
                            transaction.create(snapshot.reference, content)
                        }
                    }
                },
            ) { it.run() }
        }.await()
    }

    override suspend fun saveKotlinBlogTldrs(tldrs: Map<String, KotlinBlogContent.Tldr>) {
        if (tldrs.isEmpty()) return

        val docRefToContentMap = tldrs.mapKeys { (id, _) ->
            firestore.collection(KotlinBlogContentCollectionPath)
                .document(id.firestoreDocumentId)
        }
        val docRefs = docRefToContentMap.keys.toTypedArray()

        firestore.runAsyncTransaction { transaction ->
            ApiFutures.transform(
                transaction.getAll(*docRefs),
                { snapshots ->
                    for (snapshot in snapshots) {
                        val tldr = checkNotNull(docRefToContentMap[snapshot.reference])
                        transaction.update(snapshot.reference, "tldr", tldr)
                    }
                },
            ) { it.run() }
        }.await()
    }
}

const val KotlinBlogContentCollectionPath = "kotlin_blog_content"
