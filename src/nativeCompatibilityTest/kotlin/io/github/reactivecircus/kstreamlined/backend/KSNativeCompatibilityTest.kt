package io.github.reactivecircus.kstreamlined.backend

import com.google.cloud.firestore.DocumentSnapshot
import io.github.reactivecircus.kstreamlined.backend.store.KotlinBlogContent
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Testcontainers
@Execution(ExecutionMode.SAME_THREAD)
class KSNativeCompatibilityTest {
    private val services get() = environment.services
    private val firestore get() = environment.firestore

    @BeforeTest
    fun setUp() {
        environment.reset()
    }

    @AfterTest
    fun tearDown() {
        assertTrue(services.unexpectedRequests.isEmpty(), "Unexpected stub requests: ${services.unexpectedRequests}")
    }

    @Test
    fun `native application starts and shuts down cleanly`() {
        withNativeApp { }
        assertTrue(services.requests("/").isEmpty())
    }

    @Test
    fun `cold feedEntries query parses all feeds and round-trips them through Firestore and Redis`() {
        withNativeApp { graphql ->
            val response = graphql.documentName("feedEntries").execute()
            val types = response.path("feedEntries[*].__typename").entityList<String>().get()
            assertEquals(FeedCollections.keys.associateWith { 2 }, types.groupingBy { it }.eachCount())
            assertAllFieldsResolved(response.path("feedEntries").entityList<Map<String, Any>>().get())
            response.path("feedEntries[?(@.__typename == 'TalkingKotlin')].duration").entityList<String>()
                .containsExactly("57min.", "51min.")
                .path("feedEntries[?(@.__typename == 'KotlinWeekly')].issueNumber")
                .entityList(Int::class.javaObjectType)
                .containsExactly(381, 380)
        }
        FeedCollections.values.forEach { assertEquals(2, documents(it).size, it) }
        documents(BlogContent).forEach { assertNotNull(it.getString("html")) }
        assertEquals(4, services.requests("/feeds/").size)
        assertEquals(4, services.requests("/redis/set/").size)
    }

    @Test
    fun `feedEntries query with a cold application deserializes all feeds from Redis`() {
        RedisFixtures.forEach { (key, fixture) -> services.seedRedis(key, fixture) }

        withNativeApp { graphql ->
            val response = graphql.documentName("feedEntries").execute()
            response.path("feedEntries[*].title").entityList<String>().containsExactly(
                "A cached Kotlin article",
                "A cached Kotlin video",
                "A cached Kotlin podcast",
                "A cached Kotlin Weekly issue",
            )
            assertAllFieldsResolved(response.path("feedEntries").entityList<Map<String, Any>>().get())
        }
        assertEquals(4, services.requests("/redis/get/").size)
        assertTrue(services.requests("/feeds/").isEmpty())
        assertTrue(services.requests("/redis/set/").isEmpty())
        (FeedCollections.values + BlogContent).forEach { assertTrue(documents(it).isEmpty(), it) }
    }

    @Test
    fun `generateKotlinBlogTldr mutation persists a nested summary to Firestore`() {
        seedBlogContent(tldr = null)

        withNativeApp { graphql ->
            graphql.documentName("generateKotlinBlogTldr").variable("id", BlogId).variable("persist", true).execute()
                .path("generateKotlinBlogTldr.content").entity<String>().isEqualTo(Summary)
                .path("generateKotlinBlogTldr.model").entity<String>().isEqualTo("gpt-oss-120b")
        }
        val saved = blogContent()
        assertEquals(Summary, saved.getString("tldr.output"))
        assertEquals("gpt-oss-120b", saved.getString("tldr.model"))
        assertNotNull(saved.getTimestamp("tldr.generatedAt"))
        assertEquals(100L, saved.getLong("tldr.promptTokens"))
        assertEquals(20L, saved.getLong("tldr.completionTokens"))
        assertEquals(120L, saved.getLong("tldr.totalTokens"))
        assertEquals(1.5, saved.getDouble("tldr.neurons"))
        assertNotNull(saved.getLong("tldr.generationDurationMs"))
        val request = checkNotNull(services.requests("/ai/").single().body).utf8()
        assertTrue(request.contains("Incremental compilation"), request)
    }

    @Test
    fun `kotlinBlogTldr query reads an existing nested summary from Firestore`() {
        val summary = KotlinBlogContent.Tldr(
            output = "Previously generated summary.",
            model = "stored-model",
            generatedAt = Instant.parse("2026-09-14T12:00:00.123456789Z"),
            promptTokens = 100,
            completionTokens = 20,
            totalTokens = 120,
            neurons = 1.5,
            generationDurationMs = 250,
        )
        seedBlogContent(tldr = summary)

        withNativeApp { graphql ->
            graphql.documentName("kotlinBlogTldr").variable("id", BlogId).execute()
                .path("kotlinBlogTldr.id").entity<String>().isEqualTo(BlogId)
                .path("kotlinBlogTldr.content").entity<String>().isEqualTo(summary.output)
                .path("kotlinBlogTldr.model").entity<String>().isEqualTo(summary.model)
                .path("kotlinBlogTldr.generatedAt").entity<String>()
                .isEqualTo(summary.generatedAt.truncatedTo(ChronoUnit.MICROS).toString())
        }
        assertTrue(services.requests("/ai/").isEmpty())
    }

    @Test
    fun `kotlinWeeklyIssue query parses an issue page`() {
        withNativeApp { graphql ->
            graphql.documentName("kotlinWeeklyIssue")
                .variable("url", "${services.baseUrl}/weekly-issue")
                .execute()
                .path("kotlinWeeklyIssue[*].title").entityList<String>().hasSize(12)
                .path("kotlinWeeklyIssue[0].title").entity<String>().isEqualTo("Amper Update \u2013 December 2023")
                .path("kotlinWeeklyIssue[0].url").entity<String>()
                .isEqualTo("https://blog.jetbrains.com/amper/2023/12/amper-update-december-2023/")
                .path("kotlinWeeklyIssue[0].source").entity<String>().isEqualTo("blog.jetbrains.com")
                .path("kotlinWeeklyIssue[*].group").entityList<String>()
                .contains("NEWS", "ARTICLES", "ANDROID", "VIDEOS", "LIBRARIES")
        }
        assertEquals(1, services.requests("/weekly-issue").size)
    }

    @Test
    fun `generateKotlinBlogTldr mutation reports a rejected AI response as a GraphQL error`() {
        seedBlogContent(tldr = null)
        services.stubAiResponse(ServiceHttpStubs.AiResponse.Rejected)

        withNativeApp { graphql ->
            graphql.documentName("generateKotlinBlogTldr").variable("id", BlogId).variable("persist", true).execute()
                .errors().satisfy { errors ->
                    val message = assertNotNull(errors.single().message)
                    assertTrue(message.contains("Error codes: 429"), message)
                }
        }
        assertNull(blogContent().get("tldr"))
        assertEquals(1, services.requests("/ai/").size)
    }

    private fun withNativeApp(block: (HttpGraphQlTester) -> Unit) {
        val executable = Path.of(checkNotNull(System.getProperty("ks.native.executable")))
        val application = NativeAppProcess.start(executable, environment)
        val failure = runCatching {
            block(
                HttpGraphQlTester.create(
                    WebTestClient.bindToServer()
                        .baseUrl("${application.baseUrl}/graphql")
                        .responseTimeout(Duration.ofSeconds(5))
                        .build(),
                ),
            )
        }.exceptionOrNull()
        try {
            application.close(keepLog = failure != null)
        } catch (closeFailure: IllegalStateException) {
            failure?.let(closeFailure::addSuppressed)
            throw closeFailure
        }
        failure?.let {
            it.addSuppressed(IllegalStateException(application.diagnostics("Native application output.")))
            throw it
        }
    }

    private fun assertAllFieldsResolved(entries: List<Map<String, Any>>) {
        assertTrue(entries.isNotEmpty())
        entries.forEach { entry ->
            entry.forEach { (field, value) ->
                assertTrue(value.toString().isNotBlank(), "Blank $field in $entry")
            }
        }
    }

    private fun seedBlogContent(tldr: KotlinBlogContent.Tldr?) {
        val content = KotlinBlogContent(
            id = BlogId,
            title = "A New Approach to Incremental Compilation in Kotlin",
            html = """
                |<h2>Incremental compilation</h2>
                |<p>Kotlin 2.0 makes <a href="https://kotlinlang.org">builds</a> faster.</p>
                |<ul><li>Smaller rebuilds</li><li>Better caching</li></ul>
                |<pre><code class="language-kotlin">fun main() = println("Hello")</code></pre>
            """.trimMargin(),
            tldr = tldr,
        )
        firestore.collection(BlogContent).document(BlogDocumentId).set(content).get(5, TimeUnit.SECONDS)
    }

    private fun documents(collection: String): List<DocumentSnapshot> =
        firestore.collection(collection).get().get(5, TimeUnit.SECONDS).documents

    private fun blogContent(): DocumentSnapshot =
        firestore.collection(BlogContent).document(BlogDocumentId).get().get(5, TimeUnit.SECONDS)

    companion object {
        @Container
        private val emulator = firestoreEmulator()

        private val environment by lazy { TestEnvironment(emulator.emulatorEndpoint, "demo-ks-native") }

        private const val BlogContent = "kotlin_blog_content"
        private const val BlogDocumentId = "264203"
        private const val BlogId = "https://blog.jetbrains.com/?post_type=kotlin&p=$BlogDocumentId"
        private const val Summary = "Test summary."
        private val FeedCollections = mapOf(
            "KotlinBlog" to "kotlin_blog_feed",
            "KotlinYouTube" to "kotlin_youtube_feed",
            "TalkingKotlin" to "talking_kotlin_feed",
            "KotlinWeekly" to "kotlin_weekly_feed",
        )
        private val RedisFixtures = mapOf(
            "kotlin-blog" to "http-stubs/redis-kotlin-blog.json",
            "kotlin-youtube" to "http-stubs/redis-kotlin-youtube.json",
            "talking-kotlin" to "http-stubs/redis-talking-kotlin.json",
            "kotlin-weekly" to "http-stubs/redis-kotlin-weekly.json",
        )

        @JvmStatic
        @AfterAll
        fun closeEnvironment() {
            environment.close()
        }
    }
}
