package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinWeeklyEntryMapperTest {

    @Test
    fun `toKotlinWeeklyEntry() converts KotlinWeeklyItem to KotlinWeekly`() {
        val expected = KotlinWeekly(
            id = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
            title = "Kotlin Weekly #381",
            publishTimestamp = "1700385180",
            contentUrl = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
            newsletterLogoUrl = "logo-url",
        )
        val actual = KotlinWeeklyItem(
            title = "Kotlin Weekly #381",
            link = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
            guid = "https://mailchi.mp/kotlinweekly/kotlin-weekly-381",
            pubDate = "Sun, 19 Nov 2023 09:13:00 +0000",
        ).toKotlinWeeklyEntry(logoUrl = "logo-url")

        assertEquals(expected, actual)
    }
}
