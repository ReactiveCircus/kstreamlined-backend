package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinWeeklyIssueEntries
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeKotlinWeeklyIssueDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.KotlinWeeklyIssueDataSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(classes = [KotlinWeeklyIssueDataFetcher::class, InstantScalar::class])
@EnableAutoConfiguration
@ContextConfiguration(classes = [TestKSConfiguration::class])
class KotlinWeeklyIssueDataFetcherTest {
    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var kotlinWeeklyIssueDataSource: KotlinWeeklyIssueDataSource

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
    fun `kotlinWeeklyIssue(url) query returns expected kotlin weekly issue entries when operation succeeds`() {
        (kotlinWeeklyIssueDataSource as FakeKotlinWeeklyIssueDataSource).nextKotlinWeeklyIssueResponse = {
            DummyKotlinWeeklyIssueEntries
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            kotlinWeeklyIssueQuery,
            mapOf("url" to "https://mailchi.mp/kotlinweekly/kotlin-weekly-386"),
        )

        assertEquals(5, context.read<Int>("data.kotlinWeeklyIssue.size()"))

        assertEquals(DummyKotlinWeeklyIssueEntries[0].title, context.read<String>("data.kotlinWeeklyIssue[0].title"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[0].summary,
            context.read<String>("data.kotlinWeeklyIssue[0].summary"),
        )
        assertEquals(DummyKotlinWeeklyIssueEntries[0].url, context.read<String>("data.kotlinWeeklyIssue[0].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[0].source, context.read<String>("data.kotlinWeeklyIssue[0].source"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[0].group.name,
            context.read<String>("data.kotlinWeeklyIssue[0].group"),
        )

        assertEquals(DummyKotlinWeeklyIssueEntries[1].title, context.read<String>("data.kotlinWeeklyIssue[1].title"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[1].summary,
            context.read<String>("data.kotlinWeeklyIssue[1].summary"),
        )
        assertEquals(DummyKotlinWeeklyIssueEntries[1].url, context.read<String>("data.kotlinWeeklyIssue[1].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[1].source, context.read<String>("data.kotlinWeeklyIssue[1].source"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[1].group.name,
            context.read<String>("data.kotlinWeeklyIssue[1].group"),
        )

        assertEquals(DummyKotlinWeeklyIssueEntries[2].title, context.read<String>("data.kotlinWeeklyIssue[2].title"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[2].summary,
            context.read<String>("data.kotlinWeeklyIssue[2].summary"),
        )
        assertEquals(DummyKotlinWeeklyIssueEntries[2].url, context.read<String>("data.kotlinWeeklyIssue[2].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[2].source, context.read<String>("data.kotlinWeeklyIssue[2].source"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[2].group.name,
            context.read<String>("data.kotlinWeeklyIssue[2].group"),
        )

        assertEquals(DummyKotlinWeeklyIssueEntries[3].title, context.read<String>("data.kotlinWeeklyIssue[3].title"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[3].summary,
            context.read<String>("data.kotlinWeeklyIssue[3].summary"),
        )
        assertEquals(DummyKotlinWeeklyIssueEntries[3].url, context.read<String>("data.kotlinWeeklyIssue[3].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[3].source, context.read<String>("data.kotlinWeeklyIssue[3].source"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[3].group.name,
            context.read<String>("data.kotlinWeeklyIssue[3].group"),
        )

        assertEquals(DummyKotlinWeeklyIssueEntries[4].title, context.read<String>("data.kotlinWeeklyIssue[4].title"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[4].summary,
            context.read<String>("data.kotlinWeeklyIssue[4].summary"),
        )
        assertEquals(DummyKotlinWeeklyIssueEntries[4].url, context.read<String>("data.kotlinWeeklyIssue[4].url"))
        assertEquals(DummyKotlinWeeklyIssueEntries[4].source, context.read<String>("data.kotlinWeeklyIssue[4].source"))
        assertEquals(
            DummyKotlinWeeklyIssueEntries[4].group.name,
            context.read<String>("data.kotlinWeeklyIssue[4].group"),
        )
    }
}
