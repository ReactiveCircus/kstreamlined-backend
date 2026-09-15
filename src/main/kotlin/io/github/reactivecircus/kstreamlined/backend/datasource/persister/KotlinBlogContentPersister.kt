package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.api.core.ApiFutures
import com.google.cloud.firestore.FieldMask
import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.NoArg
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem

interface KotlinBlogContentPersister {
    suspend fun loadKotlinBlogContent(id: String): KotlinBlogContent?

    suspend fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>)
}

@NoArg
data class KotlinBlogContent(
    val title: String,
    val html: String,
) {
    companion object {
        fun from(item: KotlinBlogItem): KotlinBlogContent {
            return KotlinBlogContent(
                title = item.title,
                html = requireNotNull(item.html?.trim()),
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

    override suspend fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>) {
        val contents = items.map { item ->
            item.firestoreDocumentId to KotlinBlogContent.from(item)
        }
        if (contents.isEmpty()) return

        val documentReferences = contents.map { (documentId) ->
            firestore.collection(KotlinBlogContentCollectionPath).document(documentId)
        }
        firestore.runAsyncTransaction { transaction ->
            ApiFutures.transform(
                transaction.getAll(
                    documentReferences.toTypedArray(),
                    FieldMask.of(*emptyArray<String>()),
                ),
                { snapshots ->
                    val existingDocumentIds = snapshots
                        .filter { it.exists() }
                        .mapTo(mutableSetOf()) { it.id }

                    contents.zip(documentReferences).forEach { (content, documentReference) ->
                        if (documentReference.id !in existingDocumentIds) {
                            transaction.create(documentReference, content.second)
                        }
                    }
                },
            ) { it.run() }
        }.await()
    }
}

const val KotlinBlogContentCollectionPath = "kotlin_blog_content"
