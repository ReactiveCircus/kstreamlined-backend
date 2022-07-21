package io.github.reactivecircus.kstreamlined.backend.feedsource

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals

@SpringBootTest(classes = [DgsAutoConfiguration::class, FeedSourceDataFetcher::class])
@ContextConfiguration(classes = [TestKSConfiguration::class])
class FeedSourceDataFetcherTest {

    @Autowired
    private lateinit var dgsQueryExecutor: DgsQueryExecutor

    private val feedSourcesQuery = """
        query FeedSources {
            feedSources {
                key
                title
                description
            }
        }
    """.trimIndent()

    @Test
    fun `feedSources query returns all available feed sources`() {
        val context = dgsQueryExecutor.executeAndGetDocumentContext(feedSourcesQuery)

        assertEquals(FeedSourceKey.KOTLIN_BLOG.name, context.read("data.feedSources[0].key"))
        assertEquals(FeedSourceTitle.KotlinBlog, context.read("data.feedSources[0].title"))
        assertEquals(FeedSourceDescription.KotlinBlog, context.read("data.feedSources[0].description"))

        assertEquals(FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL.name, context.read("data.feedSources[1].key"))
        assertEquals(FeedSourceTitle.KotlinYouTube, context.read("data.feedSources[1].title"))
        assertEquals(FeedSourceDescription.KotlinYouTube, context.read("data.feedSources[1].description"))

        assertEquals(FeedSourceKey.TALKING_KOTLIN_PODCAST.name, context.read("data.feedSources[2].key"))
        assertEquals(FeedSourceTitle.TalkingKotlin, context.read("data.feedSources[2].title"))
        assertEquals(FeedSourceDescription.TalkingKotlin, context.read("data.feedSources[2].description"))

        assertEquals(FeedSourceKey.KOTLIN_WEEKLY.name, context.read("data.feedSources[3].key"))
        assertEquals(FeedSourceTitle.KotlinWeekly, context.read("data.feedSources[3].title"))
        assertEquals(FeedSourceDescription.KotlinWeekly, context.read("data.feedSources[3].description"))
    }
}
