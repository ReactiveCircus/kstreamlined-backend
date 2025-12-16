package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals

@SpringBootTest(classes = [FeedSourceDataFetcher::class, InstantScalar::class])
@EnableAutoConfiguration
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

        assertEquals(FeedSourceKey.KOTLIN_BLOG.name, context.read<String>("data.feedSources[0].key"))
        assertEquals(FeedSourceTitle.KotlinBlog, context.read<String>("data.feedSources[0].title"))
        assertEquals(FeedSourceDescription.KotlinBlog, context.read<String>("data.feedSources[0].description"))

        assertEquals(FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL.name, context.read<String>("data.feedSources[1].key"))
        assertEquals(FeedSourceTitle.KotlinYouTube, context.read<String>("data.feedSources[1].title"))
        assertEquals(FeedSourceDescription.KotlinYouTube, context.read<String>("data.feedSources[1].description"))

        assertEquals(FeedSourceKey.TALKING_KOTLIN_PODCAST.name, context.read<String>("data.feedSources[2].key"))
        assertEquals(FeedSourceTitle.TalkingKotlin, context.read<String>("data.feedSources[2].title"))
        assertEquals(FeedSourceDescription.TalkingKotlin, context.read<String>("data.feedSources[2].description"))

        assertEquals(FeedSourceKey.KOTLIN_WEEKLY.name, context.read<String>("data.feedSources[3].key"))
        assertEquals(FeedSourceTitle.KotlinWeekly, context.read<String>("data.feedSources[3].title"))
        assertEquals(FeedSourceDescription.KotlinWeekly, context.read<String>("data.feedSources[3].description"))
    }
}
