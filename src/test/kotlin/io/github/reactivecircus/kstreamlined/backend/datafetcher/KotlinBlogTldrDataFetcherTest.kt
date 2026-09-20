package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.GraphqlErrorException
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinBlogTldr
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeKotlinBlogTldrDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinBlogTldrBackfillResult
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinBlogTldrDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [KotlinBlogTldrDataFetcher::class, InstantScalar::class])
@EnableAutoConfiguration
@ContextConfiguration(classes = [TestKSConfiguration::class])
class KotlinBlogTldrDataFetcherTest {
    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var kotlinBlogTldrDataSource: KotlinBlogTldrDataSource

    private val articleId = "https://blog.jetbrains.com/?post_type=kotlin&p=12345"

    private val kotlinBlogTldrQuery = """
        query KotlinBlogTldr(${"$"}id: ID!) {
            kotlinBlogTldr(id: ${"$"}id) {
                id
                content
                model
                generatedAt
            }
        }
    """.trimIndent()

    private val generateKotlinBlogTldrMutation = """
        mutation GenerateKotlinBlogTldr(${"$"}id: ID!, ${"$"}persist: Boolean! = false) {
            generateKotlinBlogTldr(id: ${"$"}id, persist: ${"$"}persist) {
                id
                content
                model
                generatedAt
            }
        }
    """.trimIndent()

    private val backfillKotlinBlogTldrsMutation = """
        mutation Backfill {
            backfillKotlinBlogTldrs {
                generatedCount
                failedIds
            }
        }
    """.trimIndent()

    @Test
    fun `kotlinBlogTldr(id) query returns expected TLDR when operation succeeds`() {
        var requestedId: String? = null
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextKotlinBlogTldrResponse = { id ->
            requestedId = id
            DummyKotlinBlogTldr
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            kotlinBlogTldrQuery,
            mapOf("id" to articleId),
        )

        assertEquals(articleId, requestedId)
        assertEquals(articleId, context.read("data.kotlinBlogTldr.id"))
        assertEquals(DummyKotlinBlogTldr.output, context.read("data.kotlinBlogTldr.content"))
        assertEquals(DummyKotlinBlogTldr.model, context.read("data.kotlinBlogTldr.model"))
        assertEquals(DummyKotlinBlogTldr.generatedAt.toString(), context.read("data.kotlinBlogTldr.generatedAt"))
    }

    @Test
    fun `kotlinBlogTldr(id) query returns null when content is unavailable`() {
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextKotlinBlogTldrResponse = { null }

        val result = dgsQueryExecutor.execute(kotlinBlogTldrQuery, mapOf("id" to articleId))

        assertTrue(result.errors.isEmpty())
        assertEquals(mapOf("kotlinBlogTldr" to null), result.getData<Map<String, Any?>>())
    }

    @Test
    fun `kotlinBlogTldr(id) query returns error response when loading fails`() {
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextKotlinBlogTldrResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }

        val result = dgsQueryExecutor.execute(kotlinBlogTldrQuery, mapOf("id" to articleId))

        assertEquals("INTERNAL", result.errors[0].extensions["errorType"])
    }

    @Test
    fun `generateKotlinBlogTldr mutation returns expected TLDR when operation succeeds`() {
        for (persist in listOf(false, true)) {
            var requestedId: String? = null
            var requestedPersist: Boolean? = null
            (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextGenerateKotlinBlogTldrResponse =
                { id, save ->
                    requestedId = id
                    requestedPersist = save
                    DummyKotlinBlogTldr
                }

            val context = dgsQueryExecutor.executeAndGetDocumentContext(
                generateKotlinBlogTldrMutation,
                mapOf("id" to articleId, "persist" to persist),
            )

            assertEquals(articleId, requestedId)
            assertEquals(persist, requestedPersist)
            assertEquals(articleId, context.read("data.generateKotlinBlogTldr.id"))
            assertEquals(DummyKotlinBlogTldr.output, context.read("data.generateKotlinBlogTldr.content"))
            assertEquals(DummyKotlinBlogTldr.model, context.read("data.generateKotlinBlogTldr.model"))
            assertEquals(
                DummyKotlinBlogTldr.generatedAt.toString(),
                context.read("data.generateKotlinBlogTldr.generatedAt"),
            )
        }
    }

    @Test
    fun `generateKotlinBlogTldr mutation returns error response when generation fails`() {
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextGenerateKotlinBlogTldrResponse = { _, _ ->
            throw GraphqlErrorException.newErrorException().build()
        }

        val result = dgsQueryExecutor.execute(generateKotlinBlogTldrMutation, mapOf("id" to articleId))

        assertEquals("INTERNAL", result.errors[0].extensions["errorType"])
    }

    @Test
    fun `backfillKotlinBlogTldrs mutation returns expected result when operation succeeds`() {
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextBackfillKotlinBlogTldrsResponse = {
            KotlinBlogTldrBackfillResult(2, listOf("id1", "id2"))
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(backfillKotlinBlogTldrsMutation)

        assertEquals(2, context.read("data.backfillKotlinBlogTldrs.generatedCount"))
        assertEquals(listOf("id1", "id2"), context.read("data.backfillKotlinBlogTldrs.failedIds"))
    }

    @Test
    fun `backfillKotlinBlogTldrs mutation returns error response when operation fails`() {
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextBackfillKotlinBlogTldrsResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }

        val result = dgsQueryExecutor.execute(backfillKotlinBlogTldrsMutation)

        assertEquals("INTERNAL", result.errors[0].extensions["errorType"])
    }
}
