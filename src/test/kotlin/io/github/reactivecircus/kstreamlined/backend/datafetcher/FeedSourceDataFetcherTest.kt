package io.github.reactivecircus.kstreamlined.backend.datafetcher

import com.netflix.graphql.dgs.DgsQueryExecutor
import com.netflix.graphql.dgs.autoconfig.DgsAutoConfiguration
import io.github.reactivecircus.kstreamlined.backend.TestKSConfiguration
import io.github.reactivecircus.kstreamlined.backend.datafetcher.scalar.InstantScalar
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.Test

@SpringBootTest(classes = [DgsAutoConfiguration::class, FeedSourceDataFetcher::class, InstantScalar::class])
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

        assert(context.read<String>("data.feedSources[0].key") == FeedSourceKey.KOTLIN_BLOG.name)
        assert(context.read<String>("data.feedSources[0].title") == FeedSourceTitle.KotlinBlog)
        assert(context.read<String>("data.feedSources[0].description") == FeedSourceDescription.KotlinBlog)

        assert(context.read<String>("data.feedSources[1].key") == FeedSourceKey.KOTLIN_YOUTUBE_CHANNEL.name)
        assert(context.read<String>("data.feedSources[1].title") == FeedSourceTitle.KotlinYouTube)
        assert(context.read<String>("data.feedSources[1].description") == FeedSourceDescription.KotlinYouTube)

        assert(context.read<String>("data.feedSources[2].key") == FeedSourceKey.TALKING_KOTLIN_PODCAST.name)
        assert(context.read<String>("data.feedSources[2].title") == FeedSourceTitle.TalkingKotlin)
        assert(context.read<String>("data.feedSources[2].description") == FeedSourceDescription.TalkingKotlin)

        assert(context.read<String>("data.feedSources[3].key") == FeedSourceKey.KOTLIN_WEEKLY.name)
        assert(context.read<String>("data.feedSources[3].title") == FeedSourceTitle.KotlinWeekly)
        assert(context.read<String>("data.feedSources[3].description") == FeedSourceDescription.KotlinWeekly)
    }
}
