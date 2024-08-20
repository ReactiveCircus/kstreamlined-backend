package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntry
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeeklyIssueEntryGroup
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RealKotlinWeeklyIssueDataSourceTest {

    private val mockKotlinWeeklyIssueResponse =
        javaClass.classLoader.getResource("kotlin_weekly_issue_sample.html")?.readText()!!

    @Test
    fun `loadKotlinWeeklyIssue(url) returns KotlinWeeklyIssueEntry when API call succeeds`() = runBlocking {
        val mockEngine = MockEngine {
            respond(content = ByteReadChannel(mockKotlinWeeklyIssueResponse))
        }
        val kotlinWeeklyIssueDataSource = RealKotlinWeeklyIssueDataSource(mockEngine)

        val expected = listOf(
            KotlinWeeklyIssueEntry(
                title = "Amper Update – December 2023",
                summary = "Last month JetBrains introduced Amper, a tool to improve the project configuration user experience. Marton Braun gives us an update about its state in December 2023.",
                url = "https://blog.jetbrains.com/amper/2023/12/amper-update-december-2023/",
                source = "blog.jetbrains.com",
                group = KotlinWeeklyIssueEntryGroup.ANNOUNCEMENTS,
            ),
            KotlinWeeklyIssueEntry(
                title = "Koin Wrapped- Recapping the 2023 Milestones of Our Kotlin Integration Framework",
                summary = "In this post, the Koin crew wraps up all the milestones and roadmap achieved in 2023.",
                url = "https://blog.cloud-inject.io/koin-2023-highlights",
                source = "blog.cloud-inject.io",
                group = KotlinWeeklyIssueEntryGroup.ANNOUNCEMENTS,
            ),
            KotlinWeeklyIssueEntry(
                title = "How to Use the Cucumber Framework to Test Application Use Cases",
                summary = "Matthias Schenk writes today about Cucumber, a framework that can be used in application development to verify the correct behavior of the application.",
                url = "https://towardsdev.com/how-to-use-the-cucumber-framework-to-test-application-use-cases-48b4f21ee0d0",
                source = "towardsdev.com",
                group = KotlinWeeklyIssueEntryGroup.ARTICLES,
            ),
            KotlinWeeklyIssueEntry(
                title = "Jetpack Preferences DataStore in Kotlin Multiplatform (KMP)",
                summary = "FunkyMuse put up an article showcasing how to read and write preferences on multiple platforms when using the KMP DataStore library.",
                url = "https://funkymuse.dev/posts/create-data-store-kmp/",
                source = "funkymuse.dev",
                group = KotlinWeeklyIssueEntryGroup.ARTICLES,
            ),
            KotlinWeeklyIssueEntry(
                title = "Using launcher and themed icons in Android Studio, the manual way",
                summary = "Marlon Lòpez describes how we can use the launcher and the themed icons in Android Studio.",
                url = "https://dev.to/marlonlom/using-launcher-and-themed-icons-in-android-studio-the-manual-way-1h2a",
                source = "dev.to",
                group = KotlinWeeklyIssueEntryGroup.ANDROID,
            ),
            KotlinWeeklyIssueEntry(
                title = "Setting Sail with Compose Multiplatform by Isuru Rajapakse",
                summary = "Isuru Rajapakse talks at the DevFest Sri Lanka about Compose Multiplatform.",
                url = "https://www.youtube.com/watch?v=sG60644C47I",
                source = "www.youtube.com",
                group = KotlinWeeklyIssueEntryGroup.VIDEOS,
            ),
            KotlinWeeklyIssueEntry(
                title = "Developer Experience and Kotlin Lenses",
                summary = "In his next video, Duncan McGregor keeps talking about Developer Experience and Kotlin lenses.",
                url = "https://www.youtube.com/watch?v=htvpwOKYhNs",
                source = "www.youtube.com",
                group = KotlinWeeklyIssueEntryGroup.VIDEOS,
            ),
            KotlinWeeklyIssueEntry(
                title = "Network-Resilient Applications with Store5 | Talking Kotlin",
                summary = "In this chapter of Talking Kotlin, Mike Nakhimovich, Yigit Boyar, and Matthew Ramotar talk about Store, a Kotlin Multiplatform library for building network-resilient applications.",
                url = "https://www.youtube.com/watch?v=a32Otwx7c0w",
                source = "www.youtube.com",
                group = KotlinWeeklyIssueEntryGroup.VIDEOS,
            ),
            KotlinWeeklyIssueEntry(
                title = "Kim - Kotlin Image Metadata",
                summary = "Kim is a Kotlin image metadata manipulation library for Kotlin Multiplatform.",
                url = "https://github.com/Ashampoo/kim",
                source = "github.com",
                group = KotlinWeeklyIssueEntryGroup.LIBRARIES,
            ),
            KotlinWeeklyIssueEntry(
                title = "DiKTat",
                summary = "DiKTat is a strict coding standard for Kotlin, consisting of a collection of Kotlin code style rules implemented as Abstract Syntax Tree (AST) visitors built on top of KTlint.",
                url = "https://github.com/saveourtool/diktat",
                source = "github.com",
                group = KotlinWeeklyIssueEntryGroup.LIBRARIES,
            ),
            KotlinWeeklyIssueEntry(
                title = "FailGood",
                summary = "Failgood is a test runner for Kotlin focusing on simplicity, usability and speed.",
                url = "https://github.com/failgood/failgood",
                source = "github.com",
                group = KotlinWeeklyIssueEntryGroup.LIBRARIES,
            ),
            KotlinWeeklyIssueEntry(
                title = "exif-viewer",
                summary = "Free online EXIF Viewer built with Kotlin/WASM.",
                url = "https://github.com/StefanOltmann/exif-viewer",
                source = "github.com",
                group = KotlinWeeklyIssueEntryGroup.LIBRARIES,
            ),
        )

        assert(kotlinWeeklyIssueDataSource.loadKotlinWeeklyIssue("url") == expected)
    }

    @Test
    fun `loadKotlinWeeklyIssue(url) throws exception when API call fails`(): Unit = runBlocking {
        val mockEngine = MockEngine {
            respondError(HttpStatusCode.RequestTimeout)
        }
        val kotlinWeeklyIssueDataSource = RealKotlinWeeklyIssueDataSource(mockEngine)

        assertFailsWith<ClientRequestException> {
            kotlinWeeklyIssueDataSource.loadKotlinWeeklyIssue("url")
        }
    }
}
