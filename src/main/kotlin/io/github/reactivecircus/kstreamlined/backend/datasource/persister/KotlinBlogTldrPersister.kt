package io.github.reactivecircus.kstreamlined.backend.datasource.persister

import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.NoArg
import java.time.Instant

interface KotlinBlogTldrPersister {
    suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary?

    suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary)
}

@NoArg
data class KotlinBlogTldrSummary(
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
    override suspend fun loadKotlinBlogTldr(id: String): KotlinBlogTldrSummary? {
        return firestore.collection(KotlinBlogTldrCollectionPath)
            .document(id.firestoreDocumentId)
            .get()
            .await()
            .toObject(KotlinBlogTldrSummary::class.java)
    }

    override suspend fun saveKotlinBlogTldr(id: String, tldr: KotlinBlogTldrSummary) {
        firestore.collection(KotlinBlogTldrCollectionPath)
            .document(id.firestoreDocumentId)
            .set(tldr)
            .await()
    }
}

const val KotlinBlogTldrCollectionPath = "kotlin_blog_tldr"
