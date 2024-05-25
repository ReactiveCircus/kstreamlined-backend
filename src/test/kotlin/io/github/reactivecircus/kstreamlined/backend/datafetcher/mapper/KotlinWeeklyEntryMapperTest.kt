package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertFailsWith

class KotlinWeeklyEntryMapperTest {

    @Test
    fun `toKotlinWeeklyEntry() converts KotlinWeeklyItem to KotlinWeekly`() {
        val expected = KotlinWeekly(
            id = "https://mailchi.mp/kotlinweekly/kotlin-weekly-404",
            title = "Kotlin Weekly #404 (NOT FOUND)",
            publishTime = Instant.parse("2024-04-28T23:44:56Z"),
            contentUrl = "https://mailchi.mp/kotlinweekly/kotlin-weekly-404",
            issueNumber = 404,
        )
        val actual = KotlinWeeklyItem(
            title = "Kotlin Weekly #404 (NOT FOUND)",
            link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-404",
            guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-404",
            pubDate = "Sun, 28 Apr 2024 23:44:56 +0000",
        ).toKotlinWeeklyEntry()

        assert(expected == actual)
    }

    @Test
    fun `toKotlinWeeklyEntry() throws exception when issueNumber not found in title`() {
        assertFailsWith<IllegalStateException> {
            KotlinWeeklyItem(
                title = "Kotlin Weekly X",
                link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
                guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
                pubDate = "Sun, 19 Nov 2023 09:13:00 +0000",
            ).toKotlinWeeklyEntry()
        }
    }
}
