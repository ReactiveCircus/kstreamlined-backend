package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.NoArg
import java.time.Instant

interface KotlinBlogTldrPersister {
    suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr?

    suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldr)
}

@NoArg
data class KotlinBlogTldr(
    val content: String,
    val model: String,
    val generatedAt: Instant,
    val promptTokens: Int?,
    val completionTokens: Int?,
    val totalTokens: Int?,
    val neurons: Double?,
    val generationDurationMs: Long,
)

class FirestoreKotlinBlogTldrPersister(
    private val firestore: Firestore,
) : KotlinBlogTldrPersister {
    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldr? {
        return firestore.collection(KotlinBlogTldrCollectionPath)
            .document(id.firestoreDocumentId)
            .get()
            .await()
            .toObject(KotlinBlogTldr::class.java)
    }

    override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldr) {
        firestore.collection(KotlinBlogTldrCollectionPath)
            .document(id.firestoreDocumentId)
            .set(tldr)
            .await()
    }
}

const val KotlinBlogTldrCollectionPath = "kotlin_blog_tldr"
