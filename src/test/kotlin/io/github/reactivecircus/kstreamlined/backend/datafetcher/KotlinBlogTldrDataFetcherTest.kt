package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.GraphqlErrorException
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinBlogTldrSummary
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeKotlinBlogTldrDataSource
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

    @Test
    fun `kotlinBlogTldr(id) query returns expected TLDR when operation succeeds`() {
        var requestedId: String? = null
        (kotlinBlogTldrDataSource as FakeKotlinBlogTldrDataSource).nextKotlinBlogTldrResponse = { id ->
            requestedId = id
            DummyKotlinBlogTldrSummary
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            kotlinBlogTldrQuery,
            mapOf("id" to articleId),
        )

        assertEquals(articleId, requestedId)
        assertEquals(articleId, context.read("data.kotlinBlogTldr.id"))
        assertEquals(DummyKotlinBlogTldrSummary.content, context.read("data.kotlinBlogTldr.content"))
        assertEquals(DummyKotlinBlogTldrSummary.model, context.read("data.kotlinBlogTldr.model"))
        assertEquals(DummyKotlinBlogTldrSummary.generatedAt.toString(), context.read("data.kotlinBlogTldr.generatedAt"))
    }

    @Test
    fun `kotlinBlogTldr(id) query returns null without errors when content is unavailable`() {
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
}
