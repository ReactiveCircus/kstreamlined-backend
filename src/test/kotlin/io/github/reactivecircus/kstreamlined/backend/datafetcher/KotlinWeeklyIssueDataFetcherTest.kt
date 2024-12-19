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

        assert(context.read<Int>("data.kotlinWeeklyIssue.size()") == 5)

        assert(context.read<String>("data.kotlinWeeklyIssue[0].title") == DummyKotlinWeeklyIssueEntries[0].title)
        assert(context.read<String>("data.kotlinWeeklyIssue[0].summary") == DummyKotlinWeeklyIssueEntries[0].summary)
        assert(context.read<String>("data.kotlinWeeklyIssue[0].url") == DummyKotlinWeeklyIssueEntries[0].url)
        assert(context.read<String>("data.kotlinWeeklyIssue[0].source") == DummyKotlinWeeklyIssueEntries[0].source)
        assert(context.read<String>("data.kotlinWeeklyIssue[0].group") == DummyKotlinWeeklyIssueEntries[0].group.name)

        assert(context.read<String>("data.kotlinWeeklyIssue[1].title") == DummyKotlinWeeklyIssueEntries[1].title)
        assert(context.read<String>("data.kotlinWeeklyIssue[1].summary") == DummyKotlinWeeklyIssueEntries[1].summary)
        assert(context.read<String>("data.kotlinWeeklyIssue[1].url") == DummyKotlinWeeklyIssueEntries[1].url)
        assert(context.read<String>("data.kotlinWeeklyIssue[1].source") == DummyKotlinWeeklyIssueEntries[1].source)
        assert(context.read<String>("data.kotlinWeeklyIssue[1].group") == DummyKotlinWeeklyIssueEntries[1].group.name)

        assert(context.read<String>("data.kotlinWeeklyIssue[2].title") == DummyKotlinWeeklyIssueEntries[2].title)
        assert(context.read<String>("data.kotlinWeeklyIssue[2].summary") == DummyKotlinWeeklyIssueEntries[2].summary)
        assert(context.read<String>("data.kotlinWeeklyIssue[2].url") == DummyKotlinWeeklyIssueEntries[2].url)
        assert(context.read<String>("data.kotlinWeeklyIssue[2].source") == DummyKotlinWeeklyIssueEntries[2].source)
        assert(context.read<String>("data.kotlinWeeklyIssue[2].group") == DummyKotlinWeeklyIssueEntries[2].group.name)

        assert(context.read<String>("data.kotlinWeeklyIssue[3].title") == DummyKotlinWeeklyIssueEntries[3].title)
        assert(context.read<String>("data.kotlinWeeklyIssue[3].summary") == DummyKotlinWeeklyIssueEntries[3].summary)
        assert(context.read<String>("data.kotlinWeeklyIssue[3].url") == DummyKotlinWeeklyIssueEntries[3].url)
        assert(context.read<String>("data.kotlinWeeklyIssue[3].source") == DummyKotlinWeeklyIssueEntries[3].source)
        assert(context.read<String>("data.kotlinWeeklyIssue[3].group") == DummyKotlinWeeklyIssueEntries[3].group.name)

        assert(context.read<String>("data.kotlinWeeklyIssue[4].title") == DummyKotlinWeeklyIssueEntries[4].title)
        assert(context.read<String>("data.kotlinWeeklyIssue[4].summary") == DummyKotlinWeeklyIssueEntries[4].summary)
        assert(context.read<String>("data.kotlinWeeklyIssue[4].url") == DummyKotlinWeeklyIssueEntries[4].url)
        assert(context.read<String>("data.kotlinWeeklyIssue[4].source") == DummyKotlinWeeklyIssueEntries[4].source)
        assert(context.read<String>("data.kotlinWeeklyIssue[4].group") == DummyKotlinWeeklyIssueEntries[4].group.name)
    }
}
