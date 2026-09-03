package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.api.gax.rpc.AlreadyExistsException
import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.NoArg
import io.github.reactivecircus.kstreamlined.backend.datasource.dto.KotlinBlogItem
import java.util.concurrent.ExecutionException

interface KotlinBlogContentPersister {
    fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>)
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
    override fun saveMissingKotlinBlogContents(items: List<KotlinBlogItem>) {
        // TODO reimplement
        items.map { item ->
            firestore.collection(KotlinBlogContentCollectionPath)
                .document(item.firestoreDocumentId)
                .create(KotlinBlogContent.from(item))
        }.forEach { create ->
            try {
                create.get()
            } catch (e: ExecutionException) {
                if (e.cause !is AlreadyExistsException) {
                    throw e
                }
            }
        }
    }
}

const val KotlinBlogContentCollectionPath = "kotlin_blog_content"
