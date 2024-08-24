package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import graphql.GraphqlErrorException
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toTalkingKotlinEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyKotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.datasource.DummyTalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.datasource.FakeFeedDataSource
import io.github.reactivecircus.kstreamlined.backend.datasource.FeedDataSource
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.Test

@SpringBootTest(classes = [DgsAutoConfiguration::class, FeedEntryDataFetcher::class, InstantScalar::class])
@ContextConfiguration(classes = [TestKSConfiguration::class])
class FeedEntryDataFetcherTest {

    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var feedDataSource: FeedDataSource

    private val feedEntriesQuery = """
        query FeedEntriesQuery(${"$"}filters: [FeedSourceKey!]) {
            feedEntries(filters: ${"$"}filters) {
                id
                title
                publishTime
                contentUrl
                ... on KotlinBlog {
                    featuredImageUrl
                    description
                }
                ... on KotlinYouTube {
                    thumbnailUrl
                    description
                }
                ... on TalkingKotlin {
                    audioUrl
                    thumbnailUrl
                    summary
                    duration
                }
                ... on KotlinWeekly {
                    __typename
                    issueNumber
                }
            }
        }
    """.trimIndent()

    private val syncFeedsMutation = """
        mutation SyncFeeds {
            syncFeeds
        }
    """.trimIndent()

    @Test
    fun `feedEntries() query returns expected feed entries ordered by publish time when operation succeeds`() {
        (feedDataSource as FakeFeedDataSource).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedDataSource as FakeFeedDataSource).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(feedEntriesQuery)

        assert(context.read<Int>("data.feedEntries.size()") == 4)

        val dummyKotlinWeeklyEntry = DummyKotlinWeeklyItem.toKotlinWeeklyEntry()
        assert(context.read<String>("data.feedEntries[0].id") == dummyKotlinWeeklyEntry.id)
        assert(context.read<String>("data.feedEntries[0].title") == dummyKotlinWeeklyEntry.title)
        assert(context.read<String>("data.feedEntries[0].publishTime").toInstant() == dummyKotlinWeeklyEntry.publishTime)
        assert(context.read<String>("data.feedEntries[0].contentUrl") == dummyKotlinWeeklyEntry.contentUrl)
        assert(context.read<Int>("data.feedEntries[0].issueNumber") == dummyKotlinWeeklyEntry.issueNumber)

        val dummyKotlinBlogEntry = DummyKotlinBlogItem.toKotlinBlogEntry()
        assert(context.read<String>("data.feedEntries[1].id") == dummyKotlinBlogEntry.id)
        assert(context.read<String>("data.feedEntries[1].title") == dummyKotlinBlogEntry.title)
        assert(context.read<String>("data.feedEntries[1].publishTime").toInstant() == dummyKotlinBlogEntry.publishTime)
        assert(context.read<String>("data.feedEntries[1].contentUrl") == dummyKotlinBlogEntry.contentUrl)
        assert(context.read<String>("data.feedEntries[1].featuredImageUrl") == dummyKotlinBlogEntry.featuredImageUrl)
        assert(context.read<String>("data.feedEntries[1].description") == dummyKotlinBlogEntry.description)

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assert(context.read<String>("data.feedEntries[2].id") == dummyKotlinYouTubeEntry.id)
        assert(context.read<String>("data.feedEntries[2].title") == dummyKotlinYouTubeEntry.title)
        assert(context.read<String>("data.feedEntries[2].publishTime").toInstant() == dummyKotlinYouTubeEntry.publishTime)
        assert(context.read<String>("data.feedEntries[2].contentUrl") == dummyKotlinYouTubeEntry.contentUrl)
        assert(context.read<String>("data.feedEntries[2].thumbnailUrl") == dummyKotlinYouTubeEntry.thumbnailUrl)
        assert(context.read<String>("data.feedEntries[2].description") == dummyKotlinYouTubeEntry.description)

        val dummyTalkingKotlinEntry = DummyTalkingKotlinItem.toTalkingKotlinEntry()
        assert(context.read<String>("data.feedEntries[3].id") == dummyTalkingKotlinEntry.id)
        assert(context.read<String>("data.feedEntries[3].title") == dummyTalkingKotlinEntry.title)
        assert(context.read<String>("data.feedEntries[3].publishTime").toInstant() == dummyTalkingKotlinEntry.publishTime)
        assert(context.read<String>("data.feedEntries[3].contentUrl") == dummyTalkingKotlinEntry.contentUrl)
        assert(context.read<String>("data.feedEntries[3].audioUrl") == dummyTalkingKotlinEntry.audioUrl)
        assert(context.read<String>("data.feedEntries[3].thumbnailUrl") == dummyTalkingKotlinEntry.thumbnailUrl)
        assert(context.read<String>("data.feedEntries[3].summary") == dummyTalkingKotlinEntry.summary)
        assert(context.read<String>("data.feedEntries[3].duration") == dummyTalkingKotlinEntry.duration)
    }

    @Test
    fun `feedEntries() query returns error response when failed to load data from any feed sources`() {
        (feedDataSource as FakeFeedDataSource).nextKotlinBlogFeedResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedDataSource as FakeFeedDataSource).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val result = dgsQueryExecutor.execute(feedEntriesQuery)

        assert(result.errors[0].extensions["errorType"] == "INTERNAL")
    }

    @Test
    fun `feedEntries(filters) query returns expected feed entries from selected sources when filters are provided`() {
        (feedDataSource as FakeFeedDataSource).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedDataSource as FakeFeedDataSource).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            feedEntriesQuery,
            mapOf("filters" to listOf(FeedSourceKey.KOTLIN_BLOG, FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL)),
        )

        assert(context.read<Int>("data.feedEntries.size()") == 2)

        val dummyKotlinBlogEntry = DummyKotlinBlogItem.toKotlinBlogEntry()
        assert(context.read<String>("data.feedEntries[0].id") == dummyKotlinBlogEntry.id)
        assert(context.read<String>("data.feedEntries[0].title") == dummyKotlinBlogEntry.title)
        assert(context.read<String>("data.feedEntries[0].publishTime").toInstant() == dummyKotlinBlogEntry.publishTime)
        assert(context.read<String>("data.feedEntries[0].contentUrl") == dummyKotlinBlogEntry.contentUrl)
        assert(context.read<String>("data.feedEntries[0].featuredImageUrl") == dummyKotlinBlogEntry.featuredImageUrl)
        assert(context.read<String>("data.feedEntries[0].description") == dummyKotlinBlogEntry.description)

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assert(context.read<String>("data.feedEntries[1].id") == dummyKotlinYouTubeEntry.id)
        assert(context.read<String>("data.feedEntries[1].title") == dummyKotlinYouTubeEntry.title)
        assert(context.read<String>("data.feedEntries[1].publishTime").toInstant() == dummyKotlinYouTubeEntry.publishTime)
        assert(context.read<String>("data.feedEntries[1].contentUrl") == dummyKotlinYouTubeEntry.contentUrl)
        assert(context.read<String>("data.feedEntries[1].thumbnailUrl") == dummyKotlinYouTubeEntry.thumbnailUrl)
        assert(context.read<String>("data.feedEntries[1].description") == dummyKotlinYouTubeEntry.description)
    }

    @Test
    fun `syncFeeds mutation returns true when operation succeeds`() {
        (feedDataSource as FakeFeedDataSource).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedDataSource as FakeFeedDataSource).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(syncFeedsMutation)

        assert(context.read<Boolean>("data.syncFeeds") == true)
    }

    @Test
    fun `syncFeeds mutation returns error response when failed to load data from any feed sources`() {
        (feedDataSource as FakeFeedDataSource).nextKotlinBlogFeedResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedDataSource as FakeFeedDataSource).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedDataSource as FakeFeedDataSource).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val result = dgsQueryExecutor.execute(syncFeedsMutation)

        assert(result.errors[0].extensions["errorType"] == "INTERNAL")
    }

    private fun String.toInstant(): Instant = Instant.parse(this)
}
