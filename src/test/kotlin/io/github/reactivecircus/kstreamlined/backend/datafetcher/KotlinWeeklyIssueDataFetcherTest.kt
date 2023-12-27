package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.client.DummyKotlinWeeklyIssueEntries
import io.github.reactivecircus.kstreamlined.backend.client.FakeKotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.client.KotlinWeeklyIssueClient
import io.github.reactivecircus.kstreamlined.backend.scalar.InstantScalar
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

@SpringBootTest(classes = [DgsAutoConfiguration::class, KotlinWeeklyIssueDataFetcher::class, InstantScalar::class])
@ContextConfiguration(classes = [TestKSConfiguration::class])
class KotlinWeeklyIssueDataFetcherTest {

    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var kotlinWeeklyIssueClient: KotlinWeeklyIssueClient

    private val kotlinWeeklyIssueQuery = """
        query KotlinWeeklyIssue(${"$"}url: String!) {
            kotlinWeeklyIssue(url: ${"$"}url) {
                title
                summary
                url
                source
                group
            }
        }
    """.trimIndent()

    @Test
    fun `kotlinWeeklyIssue(url) query returns expected kotlin weekly issue entries when operation was successful`() {
        (kotlinWeeklyIssueClient as FakeKotlinWeeklyIssueClient).nextKotlinWeeklyIssueResponse = {
            DummyKotlinWeeklyIssueEntries
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            kotlinWeeklyIssueQuery,
            mapOf("url" to "https://mailchi.mp/kotlinweekly/kotlin-weekly-386"),
        )

        assertEquals(5, context.read("data.kotlinWeeklyIssue.size()"))

        assertEquals(DummyKotlinWeeklyIssueEntries[0].title, context.read("data.kotlinWeeklyIssue[0].title"))
        assertEquals(DummyKotlinWeeklyIssueEntries[0].summary, context.read("data.kotlinWeeklyIssue[0].summary"))
        assertEquals(DummyKotlinWeeklyIssueEntries[0].url, context.read("data.kotlinWeeklyIssue[0].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[0].source, context.read("data.kotlinWeeklyIssue[0].source"))
        assertEquals(DummyKotlinWeeklyIssueEntries[0].group.name, context.read("data.kotlinWeeklyIssue[0].group"))

        assertEquals(DummyKotlinWeeklyIssueEntries[1].title, context.read("data.kotlinWeeklyIssue[1].title"))
        assertEquals(DummyKotlinWeeklyIssueEntries[1].summary, context.read("data.kotlinWeeklyIssue[1].summary"))
        assertEquals(DummyKotlinWeeklyIssueEntries[1].url, context.read("data.kotlinWeeklyIssue[1].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[1].source, context.read("data.kotlinWeeklyIssue[1].source"))
        assertEquals(DummyKotlinWeeklyIssueEntries[1].group.name, context.read("data.kotlinWeeklyIssue[1].group"))

        assertEquals(DummyKotlinWeeklyIssueEntries[2].title, context.read("data.kotlinWeeklyIssue[2].title"))
        assertEquals(DummyKotlinWeeklyIssueEntries[2].summary, context.read("data.kotlinWeeklyIssue[2].summary"))
        assertEquals(DummyKotlinWeeklyIssueEntries[2].url, context.read("data.kotlinWeeklyIssue[2].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[2].source, context.read("data.kotlinWeeklyIssue[2].source"))
        assertEquals(DummyKotlinWeeklyIssueEntries[2].group.name, context.read("data.kotlinWeeklyIssue[2].group"))

        assertEquals(DummyKotlinWeeklyIssueEntries[3].title, context.read("data.kotlinWeeklyIssue[3].title"))
        assertEquals(DummyKotlinWeeklyIssueEntries[3].summary, context.read("data.kotlinWeeklyIssue[3].summary"))
        assertEquals(DummyKotlinWeeklyIssueEntries[3].url, context.read("data.kotlinWeeklyIssue[3].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[3].source, context.read("data.kotlinWeeklyIssue[3].source"))
        assertEquals(DummyKotlinWeeklyIssueEntries[3].group.name, context.read("data.kotlinWeeklyIssue[3].group"))

        assertEquals(DummyKotlinWeeklyIssueEntries[4].title, context.read("data.kotlinWeeklyIssue[4].title"))
        assertEquals(DummyKotlinWeeklyIssueEntries[4].summary, context.read("data.kotlinWeeklyIssue[4].summary"))
        assertEquals(DummyKotlinWeeklyIssueEntries[4].url, context.read("data.kotlinWeeklyIssue[4].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[4].source, context.read("data.kotlinWeeklyIssue[4].source"))
        assertEquals(DummyKotlinWeeklyIssueEntries[4].group.name, context.read("data.kotlinWeeklyIssue[4].group"))
    }
}
