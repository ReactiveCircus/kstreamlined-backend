package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import graphql.GraphqlErrorException
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.client.DummyKotlinBlogItem
import io.github.reactivecircus.kstreamlined.backend.client.DummyKotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.DummyKotlinYouTubeItem
import io.github.reactivecircus.kstreamlined.backend.client.DummyTalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.client.FakeFeedClient
import io.github.reactivecircus.kstreamlined.backend.client.FeedClient
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.KOTLIN_WEEKLY_LOGO_URL
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.TALKING_KOTLIN_LOGO_URL
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinBlogEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinWeeklyEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toKotlinYouTubeEntry
import io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper.toTalkingKotlinEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

@SpringBootTest(classes = [DgsAutoConfiguration::class, FeedEntryDataFetcher::class])
@ContextConfiguration(classes = [TestKSConfiguration::class])
class FeedEntryDataFetcherTest {

    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    @Autowired
    private lateinit var feedClient: FeedClient

    private val feedEntriesQuery = """
        query FeedEntriesQuery(${"$"}filters: [FeedSourceKey!]) {
            feedEntries(filters: ${"$"}filters) {
                id
                title
                publishTimestamp
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
                    podcastLogoUrl
                    tags
                }
                ... on KotlinWeekly {
                    newsletterLogoUrl
                }
            }
        }
    """.trimIndent()

    @Test
    fun `feedEntries() query returns expected feed entries ordered by publish time when operation was successful`() {
        (feedClient as FakeFeedClient).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedClient as FakeFeedClient).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedClient as FakeFeedClient).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedClient as FakeFeedClient).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val context = dgsQueryExecutor.executeAndGetDocumentContext(feedEntriesQuery)

        assertEquals(4, context.read("data.feedEntries.size()"))

        val dummyKotlinWeeklyEntry = DummyKotlinWeeklyItem.toKotlinWeeklyEntry(KOTLIN_WEEKLY_LOGO_URL)
        assertEquals(dummyKotlinWeeklyEntry.id, context.read("data.feedEntries[0].id"))
        assertEquals(dummyKotlinWeeklyEntry.title, context.read("data.feedEntries[0].title"))
        assertEquals(dummyKotlinWeeklyEntry.publishTimestamp, context.read("data.feedEntries[0].publishTimestamp"))
        assertEquals(dummyKotlinWeeklyEntry.contentUrl, context.read("data.feedEntries[0].contentUrl"))
        assertEquals(dummyKotlinWeeklyEntry.newsletterLogoUrl, context.read("data.feedEntries[0].newsletterLogoUrl"))

        val dummyKotlinBlogEntry = DummyKotlinBlogItem.toKotlinBlogEntry()
        assertEquals(dummyKotlinBlogEntry.id, context.read("data.feedEntries[1].id"))
        assertEquals(dummyKotlinBlogEntry.title, context.read("data.feedEntries[1].title"))
        assertEquals(dummyKotlinBlogEntry.publishTimestamp, context.read("data.feedEntries[1].publishTimestamp"))
        assertEquals(dummyKotlinBlogEntry.contentUrl, context.read("data.feedEntries[1].contentUrl"))
        assertEquals(dummyKotlinBlogEntry.featuredImageUrl, context.read("data.feedEntries[1].featuredImageUrl"))
        assertEquals(dummyKotlinBlogEntry.description, context.read("data.feedEntries[1].description"))

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assertEquals(dummyKotlinYouTubeEntry.id, context.read("data.feedEntries[2].id"))
        assertEquals(dummyKotlinYouTubeEntry.title, context.read("data.feedEntries[2].title"))
        assertEquals(dummyKotlinYouTubeEntry.publishTimestamp, context.read("data.feedEntries[2].publishTimestamp"))
        assertEquals(dummyKotlinYouTubeEntry.contentUrl, context.read("data.feedEntries[2].contentUrl"))
        assertEquals(dummyKotlinYouTubeEntry.thumbnailUrl, context.read("data.feedEntries[2].thumbnailUrl"))
        assertEquals(dummyKotlinYouTubeEntry.description, context.read("data.feedEntries[2].description"))

        val dummyTalkingKotlinEntry = DummyTalkingKotlinItem.toTalkingKotlinEntry(TALKING_KOTLIN_LOGO_URL)
        assertEquals(dummyTalkingKotlinEntry.id, context.read("data.feedEntries[3].id"))
        assertEquals(dummyTalkingKotlinEntry.title, context.read("data.feedEntries[3].title"))
        assertEquals(dummyTalkingKotlinEntry.publishTimestamp, context.read("data.feedEntries[3].publishTimestamp"))
        assertEquals(dummyTalkingKotlinEntry.contentUrl, context.read("data.feedEntries[3].contentUrl"))
        assertEquals(dummyTalkingKotlinEntry.podcastLogoUrl, context.read("data.feedEntries[3].podcastLogoUrl"))
        assertEquals(dummyTalkingKotlinEntry.tags[0], context.read("data.feedEntries[3].tags[0]"))
        assertEquals(dummyTalkingKotlinEntry.tags[1], context.read("data.feedEntries[3].tags[1]"))
    }

    @Test
    fun `feedEntries() query returns error response when failed to load data from any feed sources`() {
        (feedClient as FakeFeedClient).nextKotlinBlogFeedResponse = {
            throw GraphqlErrorException.newErrorException().build()
        }
        (feedClient as FakeFeedClient).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedClient as FakeFeedClient).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedClient as FakeFeedClient).nextKotlinWeeklyFeedResponse = {
            listOf(DummyKotlinWeeklyItem)
        }

        val result = dgsQueryExecutor.execute(feedEntriesQuery)

        assertEquals("INTERNAL", result.errors[0].extensions["errorType"])
    }

    @Test
    fun `feedEntries(filters) query returns expected feed entries from selected sources when filters are provided`() {
        (feedClient as FakeFeedClient).nextKotlinBlogFeedResponse = {
            listOf(DummyKotlinBlogItem)
        }
        (feedClient as FakeFeedClient).nextKotlinYouTubeFeedResponse = {
            listOf(DummyKotlinYouTubeItem)
        }
        (feedClient as FakeFeedClient).nextTalkingKotlinFeedResponse = {
            listOf(DummyTalkingKotlinItem)
        }
        (feedClient as FakeFeedClient).nextKotlinWeeklyFeedResponse = {
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
        assertEquals(dummyKotlinBlogEntry.publishTimestamp, context.read("data.feedEntries[0].publishTimestamp"))
        assertEquals(dummyKotlinBlogEntry.contentUrl, context.read("data.feedEntries[0].contentUrl"))
        assertEquals(dummyKotlinBlogEntry.featuredImageUrl, context.read("data.feedEntries[0].featuredImageUrl"))
        assertEquals(dummyKotlinBlogEntry.description, context.read("data.feedEntries[0].description"))

        val dummyKotlinYouTubeEntry = DummyKotlinYouTubeItem.toKotlinYouTubeEntry()
        assertEquals(dummyKotlinYouTubeEntry.id, context.read("data.feedEntries[1].id"))
        assertEquals(dummyKotlinYouTubeEntry.title, context.read("data.feedEntries[1].title"))
        assertEquals(dummyKotlinYouTubeEntry.publishTimestamp, context.read("data.feedEntries[1].publishTimestamp"))
        assertEquals(dummyKotlinYouTubeEntry.contentUrl, context.read("data.feedEntries[1].contentUrl"))
        assertEquals(dummyKotlinYouTubeEntry.thumbnailUrl, context.read("data.feedEntries[1].thumbnailUrl"))
        assertEquals(dummyKotlinYouTubeEntry.description, context.read("data.feedEntries[1].description"))
    }
}
