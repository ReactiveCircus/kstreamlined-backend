package io.github.reactivecircus.kstreamlined.backend

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.Firestore
import io.github.reactivecircus.kstreamlined.backend.cloudflare.CloudflareAiRequest.Message.Role
import io.github.reactivecircus.kstreamlined.backend.store.KotlinBlogContent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.tester.AutoConfigureHttpGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.graphql.test.tester.HttpGraphQlTester
import org.springframework.graphql.test.tester.entity
import org.springframework.graphql.test.tester.entityList
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.gcloud.FirestoreEmulatorContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
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

@SpringBootTest(classes = [KSBackendApplication::class], webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureHttpGraphQlTester
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestMethodOrder(MethodOrderer.Random::class)
@Testcontainers
class KSIntegrationTest {
    @Autowired
    private lateinit var graphQlTester: HttpGraphQlTester

    @Autowired
    private lateinit var webTestClient: WebTestClient

    @TestBean(methodName = "emulatorFirestore", enforceOverride = true)
    private lateinit var firestore: Firestore

    private val services get() = environment.services
    private val graphql get() = graphQlTester.mutate().responseTimeout(Duration.ofSeconds(1)).build()

    @BeforeTest
    fun resetExternalState() {
        environment.reset()
    }

    @AfterTest
    fun rejectUnexpectedHttpRequests() {
        assertTrue(services.unexpectedRequests.isEmpty(), "Unexpected stub requests: ${services.unexpectedRequests}")
    }

    @Test
    fun `feedSources query exposes all keys titles and descriptions`() {
        graphql.documentName("feedSources").execute()
            .path("feedSources[*].key").entityList<String>()
            .containsExactly("KOTLIN_BLOG", "KOTLIN_YOUTUBE_CHANNEL", "TALKING_KOTLIN_PODCAST", "KOTLIN_WEEKLY")
            .path("feedSources[*].title").entityList<String>().hasSize(4)
            .path("feedSources[*].description").entityList<String>().hasSize(4)
        assertTrue(services.requests("/feeds/").isEmpty())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `cold feedEntries query parses all fixtures persists models and resolves fragments in date order`() {
        val response = graphql.documentName("feedEntries").execute()
        val types = response.path("feedEntries[*].__typename").entityList<String>().get()
        val times = response.path("feedEntries[*].publishTime").entityList<String>().get().map(Instant::parse)

        assertEquals(FeedCollections.keys.associateWith { 2 }, types.groupingBy { it }.eachCount())
        assertEquals(times.sortedDescending(), times)
        FeedCollections.values.forEach { assertEquals(2, documents(it).size) }
        assertEquals(setOf("264203", "265263"), documents(BlogContent).map { it.id }.toSet())
        documents(BlogContent).forEach { assertNotNull(it.getString("html")) }
        documents(BlogFeed).forEach { assertNull(it.get("html")) }
        response.path("feedEntries[?(@.__typename == 'KotlinBlog')].title").entityList<String>()
            .containsExactly(
                "A New Approach to Incremental Compilation in Kotlin",
                "Kotlin News: KotlinConf, Build Reports, DataFrame Preview, and More",
            )
            .path("feedEntries[?(@.__typename == 'KotlinWeekly')].issueNumber").entityList(Int::class.javaObjectType)
            .containsExactly(381, 380)
            .path("feedEntries[?(@.__typename == 'TalkingKotlin')].duration").entityList(String::class.java)
            .containsExactly("57min.", "51min.")
        assertEquals(4, services.requests("/feeds/").size)
        assertEquals(4, services.requests("/redis/set/").size)
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `feedEntries query with source filters fetches and persists only selected feeds`() {
        graphql.documentName("feedEntries")
            .variable("filters", listOf("KOTLIN_BLOG", "KOTLIN_WEEKLY"))
            .execute()
            .path("feedEntries[*].__typename").entityList<String>()
            .containsExactly("KotlinWeekly", "KotlinWeekly", "KotlinBlog", "KotlinBlog")
        assertEquals(2, services.requests("/feeds/").size)
        assertTrue(documents("kotlin_youtube_feed").isEmpty())
        assertTrue(documents("talking_kotlin_feed").isEmpty())
    }

    @Test
    fun `feedEntries query with empty source filters returns no entries without fetching feeds`() {
        graphql.documentName("feedEntries").variable("filters", emptyList<String>()).execute()
            .path("feedEntries").entityList<String>().hasSize(0)
        assertTrue(services.requests("/feeds/").isEmpty())
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `repeated feedEntries queries use the local cache and populate Redis with raw JSON`() {
        val first = graphql.documentName("feedEntries").execute()
            .path("feedEntries[*].id").entityList<String>().get()
        val second = graphql.documentName("feedEntries").execute()
            .path("feedEntries[*].id").entityList<String>().get()

        assertEquals(first, second)
        assertEquals(4, services.requests("/feeds/").size)
        assertEquals(4, services.requests("/redis/get/").size)
        services.requests("/redis/set/").forEach { request ->
            assertEquals("Bearer integration-token", request.headers["Authorization"])
            assertEquals("3600", request.url.queryParameter("EX"))
            assertEquals(2, Json.parseToJsonElement(checkNotNull(request.body).utf8()).jsonArray.size)
        }
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    fun `feedEntries query with a cold application deserializes Redis entries without fetching or persisting a feed`() {
        services.seedRedis("kotlin-blog", "http-stubs/redis-kotlin-blog.json")

        graphql.documentName("feedEntries").variable("filters", listOf("KOTLIN_BLOG")).execute()
            .path("feedEntries[*].title").entityList<String>().containsExactly("A cached Kotlin article")
            .path("feedEntries[0].id").entity<String>().isEqualTo("https://example.invalid/?p=900001")
            .path("feedEntries[0].featuredImageUrl").entity<String>()
            .isEqualTo("https://example.invalid/image.png")
        assertEquals(1, services.requests("/redis/get/kotlin-blog").size)
        assertTrue(services.requests("/feeds/").isEmpty())
        assertTrue(services.requests("/redis/set/").isEmpty())
        assertTrue(documents(BlogFeed).isEmpty())
        assertTrue(documents(BlogContent).isEmpty())
    }

    @Test
    fun `syncFeeds mutation restores missing documents without overwriting an existing summary`() {
        syncFeeds()
        generateTldr(BlogId, persist = true)
        firestore.collection(BlogFeed).document("265263").delete().get(15, TimeUnit.SECONDS)
        firestore.collection(BlogContent).document("265263").delete().get(15, TimeUnit.SECONDS)

        syncFeeds()

        assertEquals(2, documents(BlogFeed).size)
        assertTrue(document("265263").exists())
        assertNull(document("265263").get("tldr"))
        assertEquals(Summary, document("264203").getString("tldr.output"))
        assertEquals(8, services.requests("/feeds/").size)
    }

    @Test
    fun `syncFeed failures produce GraphQL errors`() {
        services.stubFeedFailure("/feeds/blog")
        graphql.documentName("syncFeeds").execute().errors().satisfy { errors ->
            assertEquals(1, errors.size)
            assertTrue(assertNotNull(errors.single().message).contains("503"))
        }
        assertTrue(documents(BlogFeed).isEmpty())
    }

    @Test
    fun `kotlinWeeklyIssue query parses kotlin weekly issue with titles, links and groups`() {
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
        assertEquals(1, services.requests("/weekly-issue").size)
    }

    @Test
    fun `kotlinBlogTldr query generates and persists a summary when article content has no summary`() {
        syncFeeds()
        assertNull(document("264203").get("tldr"))

        val generated = graphql.documentName("kotlinBlogTldr").variable("id", BlogId).execute()
        generated.path("kotlinBlogTldr.id").entity<String>().isEqualTo(BlogId)
            .path("kotlinBlogTldr.content").entity<String>().isEqualTo(Summary)
            .path("kotlinBlogTldr.model").entity<String>().isEqualTo("gpt-oss-120b")
        val generatedAt = Instant.parse(generated.path("kotlinBlogTldr.generatedAt").entity<String>().get())

        val saved = document("264203")
        assertEquals(Summary, saved.getString("tldr.output"))
        val savedAt = assertNotNull(saved.getTimestamp("tldr.generatedAt"))
        assertEquals(
            generatedAt.truncatedTo(ChronoUnit.MICROS),
            Instant.ofEpochSecond(savedAt.seconds, savedAt.nanos.toLong()),
        )
        assertEquals(100L, saved.getLong("tldr.promptTokens"))
        assertEquals(20L, saved.getLong("tldr.completionTokens"))
        assertEquals(120L, saved.getLong("tldr.totalTokens"))
        assertEquals(1.5, saved.getDouble("tldr.neurons"))
        val request = services.requests("/ai/").single()
        assertEquals("Bearer integration-token", request.headers["Authorization"])
        val messages = Json.parseToJsonElement(checkNotNull(request.body).utf8())
            .jsonObject.getValue("messages").jsonArray
        assertEquals(listOf("system", "user"), messages.map { it.jsonObject.getValue("role").jsonPrimitive.content })
        assertTrue(messages.last().jsonObject.getValue("content").jsonPrimitive.content.contains("Incremental Compilation"))
    }

    @Test
    fun `kotlinBlogTldr query returns an existing summary without AI requests`() {
        syncFeeds()
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
        firestore.collection(BlogContent).document("264203").update("tldr", summary).get(1, TimeUnit.SECONDS)
        val saved = document("264203")

        val response = graphql.documentName("kotlinBlogTldr").variable("id", BlogId).execute()
        response.path("kotlinBlogTldr.id").entity<String>().isEqualTo(BlogId)
            .path("kotlinBlogTldr.content").entity<String>().isEqualTo(summary.output)
            .path("kotlinBlogTldr.model").entity<String>().isEqualTo(summary.model)
        val generatedAt = Instant.parse(response.path("kotlinBlogTldr.generatedAt").entity<String>().get())
        assertEquals(summary.generatedAt.truncatedTo(ChronoUnit.MICROS), generatedAt)
        assertTrue(services.requests("/ai/").isEmpty())
        assertEquals(saved.updateTime, document("264203").updateTime)
    }

    @Test
    fun `kotlinBlogTldr query with missing articles returns null without AI requests`() {
        graphql.documentName("kotlinBlogTldr").variable("id", UnknownId).execute()
            .path("kotlinBlogTldr").valueIsNull()
        assertTrue(services.requests("/ai/").isEmpty())
    }

    @Test
    fun `generateKotlinBlogTldr mutation defaults to not persisting and explicit persist saves the result`() {
        syncFeeds()

        graphql.documentName("generateKotlinBlogTldr").variable("id", BlogId).execute()
            .path("generateKotlinBlogTldr.content").entity<String>().isEqualTo(Summary)
        assertNull(document("264203").get("tldr"))

        generateTldr(BlogId, persist = false)
        assertNull(document("264203").get("tldr"))
        generateTldr(BlogId, persist = true)
        assertEquals(Summary, document("264203").getString("tldr.output"))
        assertEquals(3, services.requests("/ai/").size)
    }

    @Test
    fun `generateKotlinBlogTldr mutation for a missing article reports an error without AI requests`() {
        graphql.documentName("generateKotlinBlogTldr").variable("id", UnknownId).execute()
            .errors().satisfy { errors ->
                assertEquals(1, errors.size)
                assertTrue(assertNotNull(errors.single().message).contains("Kotlin Blog content not found"))
            }
        assertTrue(services.requests("/ai/").isEmpty())
    }

    @ParameterizedTest
    @EnumSource(value = ServiceHttpStubs.AiResponse::class, names = ["Rejected", "HttpFailure"])
    fun `generateKotlinBlogTldr mutation reports AI failures without persisting a summary`(
        failure: ServiceHttpStubs.AiResponse
    ) {
        syncFeeds()
        services.stubAiResponse(failure)

        graphql.documentName("generateKotlinBlogTldr").variable("id", BlogId).variable("persist", true).execute()
            .errors().satisfy { errors ->
                assertEquals(1, errors.size)
                val expected = if (failure == ServiceHttpStubs.AiResponse.Rejected) "Error codes: 429" else "503"
                val message = assertNotNull(errors.single().message)
                assertTrue(message.contains(expected), message)
            }
        assertNull(document("264203").get("tldr"))
        assertEquals(1, services.requests("/ai/").size)
    }

    @Test
    fun `backfillKotlinBlogTldrs mutation generates missing summaries and then becomes a no-op`() {
        syncFeeds()

        assertBackfill(generatedCount = 2)
        documents(BlogContent).forEach { assertEquals(Summary, it.getString("tldr.output")) }
        assertBackfill(generatedCount = 0)
        assertEquals(2, services.requests("/ai/").size)

        firestore.collection(BlogContent).document("264203")
            .update(mapOf<String, Any?>("tldr" to null)).get(1, TimeUnit.SECONDS)
        assertBackfill(generatedCount = 1)
        assertEquals(Summary, document("264203").getString("tldr.output"))
        assertEquals(3, services.requests("/ai/").size)
    }

    @Test
    fun `backfillKotlinBlogTldrs mutation reports partial failures and saves successful summaries`() {
        syncFeeds()
        services.stubAiResponse(ServiceHttpStubs.AiResponse.Rejected) { request ->
            request.messages.any {
                it.role == Role.User &&
                    it.content.contains("A New Approach to Incremental Compilation in Kotlin")
            }
        }

        graphql.documentName("backfillKotlinBlogTldrs").execute()
            .path("backfillKotlinBlogTldrs.generatedCount").entity<Int>().isEqualTo(1)
            .path("backfillKotlinBlogTldrs.failedIds").entityList<String>().containsExactly(BlogId)
        assertNull(document("264203").get("tldr"))
        assertEquals(Summary, document("265263").getString("tldr.output"))
    }

    @Test
    fun `backfillKotlinBlogTldrs mutation with no missing summaries succeeds without AI requests`() {
        assertBackfill(generatedCount = 0)
        assertTrue(services.requests("/ai/").isEmpty())
    }

    private fun syncFeeds() {
        graphql.documentName("syncFeeds").execute().path("syncFeeds").entity<Boolean>().isEqualTo(true)
    }

    private fun generateTldr(id: String, persist: Boolean) {
        graphql.documentName("generateKotlinBlogTldr").variable("id", id).variable("persist", persist).execute()
            .path("generateKotlinBlogTldr.content").entity<String>().isEqualTo(Summary)
    }

    private fun assertBackfill(generatedCount: Int) {
        graphql.documentName("backfillKotlinBlogTldrs").execute()
            .path("backfillKotlinBlogTldrs.generatedCount").entity<Int>().isEqualTo(generatedCount)
            .path("backfillKotlinBlogTldrs.failedIds").entityList<String>().hasSize(0)
    }

    private fun documents(collection: String): List<DocumentSnapshot> =
        firestore.collection(collection).get().get(1, TimeUnit.SECONDS).documents

    private fun document(id: String): DocumentSnapshot =
        firestore.collection(BlogContent).document(id).get().get(1, TimeUnit.SECONDS)

    companion object {
        private const val FirestoreEmulatorImage = "gcr.io/google.com/cloudsdktool/google-cloud-cli:587.0.0-emulators"

        @Container
        private val emulator = FirestoreEmulatorContainer(
            DockerImageName.parse(FirestoreEmulatorImage),
        ).withCreateContainerCmdModifier { command ->
            checkNotNull(command.hostConfig).withPortBindings(
                PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 0), ExposedPort.tcp(8080)),
            )
        }

        private val environment by lazy { TestEnvironment(emulator.emulatorEndpoint) }

        private const val BlogFeed = "kotlin_blog_feed"
        private const val BlogContent = "kotlin_blog_content"
        private const val BlogId = "https://blog.jetbrains.com/?post_type=kotlin&p=264203"
        private const val UnknownId = "https://blog.jetbrains.com/?post_type=kotlin&p=0"
        private const val Summary = "Test summary."
        private val FeedCollections = mapOf(
            "KotlinBlog" to BlogFeed,
            "KotlinYouTube" to "kotlin_youtube_feed",
            "TalkingKotlin" to "talking_kotlin_feed",
            "KotlinWeekly" to "kotlin_weekly_feed",
        )

        @JvmStatic
        @DynamicPropertySource
        fun externalServiceProperties(registry: DynamicPropertyRegistry) {
            val url = environment.services.baseUrl
            registry.add("ks.kotlin-blog-feed-url") { "$url/feeds/blog" }
            registry.add("ks.kotlin-youtube-feed-url") { "$url/feeds/youtube" }
            registry.add("ks.talking-kotlin-feed-url") { "$url/feeds/podcast" }
            registry.add("ks.kotlin-weekly-feed-url") { "$url/feeds/weekly" }
            registry.add("KS_REDIS_REST_URL") { "$url/redis" }
            registry.add("KS_REDIS_REST_TOKEN") { "integration-token" }
            registry.add("KS_CF_BASE_URL") { "$url/ai" }
            registry.add("KS_CF_ACCOUNT_ID") { "integration" }
            registry.add("KS_CF_API_TOKEN") { "integration-token" }
            registry.add("KS_GCLOUD_PROJECT_ID") { TestEnvironment.ProjectId }
        }

        @JvmStatic
        fun emulatorFirestore(): Firestore = environment.firestore()

        @JvmStatic
        @AfterAll
        fun closeTestResources() {
            environment.close()
        }
    }
}
