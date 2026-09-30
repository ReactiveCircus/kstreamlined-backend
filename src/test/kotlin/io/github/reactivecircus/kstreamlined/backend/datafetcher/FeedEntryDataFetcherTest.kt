package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import graphql.GraphqlErrorException
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toTalkingKotlinEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import io.github.reactivecircus.kstreamlined.backend.service.DummyKotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.service.DummyKotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.service.DummyKotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.service.DummyTalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.service.FakeFeedService
import io.github.reactivecircus.kstreamlined.backend.service.FeedService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest(classes = [FeedEntryDataFetcher::class, InstantScalar::class])
@EnableAutoConfiguration
@ContextConfiguration(classes = [TestKSConfiguration::class])
class FeedEntryDataFetcherTest {
    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var feedService: FeedService

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
        (feedService as FakeFeedService).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedService as FakeFeedService).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedService as FakeFeedService).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedService as FakeFeedService).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(feedEntriesQuery)

        assertEquals(4, context.read("data.feedEntries.size()"))

        val dummyKotlinWeeklyEntry = DummyKotlinWeeklyItem.toKotlinWeeklyEntry()
        assertEquals(dummyKotlinWeeklyEntry.id, context.read("data.feedEntries[0].id"))
        assertEquals(dummyKotlinWeeklyEntry.title, context.read("data.feedEntries[0].title"))
        assertEquals(
            dummyKotlinWeeklyEntry.publishTime,
            context.read<String>("data.feedEntries[0].publishTime").toInstant(),
        )
        assertEquals(dummyKotlinWeeklyEntry.contentUrl, context.read("data.feedEntries[0].contentUrl"))
        assertEquals(dummyKotlinWeeklyEntry.issueNumber, context.read("data.feedEntries[0].issueNumber"))

        val dummyKotlinBlogEntry = DummyKotlinBlogItem.toKotlinBlogEntry()
        assertEquals(dummyKotlinBlogEntry.id, context.read("data.feedEntries[1].id"))
        assertEquals(dummyKotlinBlogEntry.title, context.read("data.feedEntries[1].title"))
        assertEquals(
            dummyKotlinBlogEntry.publishTime,
            context.read<String>("data.feedEntries[1].publishTime").toInstant(),
        )
        assertEquals(dummyKotlinBlogEntry.contentUrl, context.read("data.feedEntries[1].contentUrl"))
        assertEquals(
            dummyKotlinBlogEntry.featuredImageUrl,
            context.read("data.feedEntries[1].featuredImageUrl"),
        )
        assertEquals(dummyKotlinBlogEntry.description, context.read("data.feedEntries[1].description"))

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assertEquals(dummyKotlinYouTubeEntry.id, context.read("data.feedEntries[2].id"))
        assertEquals(dummyKotlinYouTubeEntry.title, context.read("data.feedEntries[2].title"))
        assertEquals(
            dummyKotlinYouTubeEntry.publishTime,
            context.read<String>("data.feedEntries[2].publishTime").toInstant(),
        )
        assertEquals(dummyKotlinYouTubeEntry.contentUrl, context.read("data.feedEntries[2].contentUrl"))
        assertEquals(dummyKotlinYouTubeEntry.thumbnailUrl, context.read("data.feedEntries[2].thumbnailUrl"))
        assertEquals(dummyKotlinYouTubeEntry.description, context.read("data.feedEntries[2].description"))

        val dummyTalkingKotlinEntry = DummyTalkingKotlinItem.toTalkingKotlinEntry()
        assertEquals(dummyTalkingKotlinEntry.id, context.read("data.feedEntries[3].id"))
        assertEquals(dummyTalkingKotlinEntry.title, context.read("data.feedEntries[3].title"))
        assertEquals(
            dummyTalkingKotlinEntry.publishTime,
            context.read<String>("data.feedEntries[3].publishTime").toInstant(),
        )
        assertEquals(dummyTalkingKotlinEntry.contentUrl, context.read("data.feedEntries[3].contentUrl"))
        assertEquals(dummyTalkingKotlinEntry.audioUrl, context.read("data.feedEntries[3].audioUrl"))
        assertEquals(dummyTalkingKotlinEntry.thumbnailUrl, context.read("data.feedEntries[3].thumbnailUrl"))
        assertEquals(dummyTalkingKotlinEntry.summary, context.read("data.feedEntries[3].summary"))
        assertEquals(dummyTalkingKotlinEntry.duration, context.read("data.feedEntries[3].duration"))
    }

    @Test
    fun `feedEntries() query returns error response when failed to load data from any feed sources`() {
        (feedService as FakeFeedService).nextKotlinBlogFeedResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }
        (feedService as FakeFeedService).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedService as FakeFeedService).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedService as FakeFeedService).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val result = dgsQueryExecutor.execute(feedEntriesQuery)

        assertEquals("INTERNAL", result.errors[0].extensions["errorType"])
    }

    @Test
    fun `feedEntries(filters) query returns expected feed entries from selected sources when filters are provided`() {
        (feedService as FakeFeedService).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedService as FakeFeedService).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedService as FakeFeedService).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedService as FakeFeedService).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(
            feedEntriesQuery,
            mapOf("filters" to listOf(FeedSourceKey.KOTLIN_BLOG, FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL)),
        )

        assertEquals(2, context.read("data.feedEntries.size()"))

        val dummyKotlinBlogEntry = DummyKotlinBlogItem.toKotlinBlogEntry()
        assertEquals(dummyKotlinBlogEntry.id, context.read("data.feedEntries[0].id"))
        assertEquals(dummyKotlinBlogEntry.title, context.read("data.feedEntries[0].title"))
        assertEquals(
            dummyKotlinBlogEntry.publishTime,
            context.read<String>("data.feedEntries[0].publishTime").toInstant(),
        )
        assertEquals(dummyKotlinBlogEntry.contentUrl, context.read("data.feedEntries[0].contentUrl"))
        assertEquals(
            dummyKotlinBlogEntry.featuredImageUrl,
            context.read("data.feedEntries[0].featuredImageUrl"),
        )
        assertEquals(dummyKotlinBlogEntry.description, context.read("data.feedEntries[0].description"))

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assertEquals(dummyKotlinYouTubeEntry.id, context.read("data.feedEntries[1].id"))
        assertEquals(dummyKotlinYouTubeEntry.title, context.read("data.feedEntries[1].title"))
        assertEquals(
            dummyKotlinYouTubeEntry.publishTime,
            context.read<String>("data.feedEntries[1].publishTime").toInstant(),
        )
        assertEquals(dummyKotlinYouTubeEntry.contentUrl, context.read("data.feedEntries[1].contentUrl"))
        assertEquals(dummyKotlinYouTubeEntry.thumbnailUrl, context.read("data.feedEntries[1].thumbnailUrl"))
        assertEquals(dummyKotlinYouTubeEntry.description, context.read("data.feedEntries[1].description"))
    }

    @Test
    fun `syncFeeds mutation returns true when operation succeeds`() {
        (feedService as FakeFeedService).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedService as FakeFeedService).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedService as FakeFeedService).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedService as FakeFeedService).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(syncFeedsMutation)

        assertTrue(context.read("data.syncFeeds"))
    }

    @Test
    fun `syncFeeds mutation returns error response when failed to load data from any feed sources`() {
        (feedService as FakeFeedService).nextKotlinBlogFeedResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }
        (feedService as FakeFeedService).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedService as FakeFeedService).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedService as FakeFeedService).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val result = dgsQueryExecutor.execute(syncFeedsMutation)

        assert(result.errors[0].extensions["errorType"] == "INTERNAL")
    }

    private fun String.toInstant(): Instant = Instant.parse(this)
}
