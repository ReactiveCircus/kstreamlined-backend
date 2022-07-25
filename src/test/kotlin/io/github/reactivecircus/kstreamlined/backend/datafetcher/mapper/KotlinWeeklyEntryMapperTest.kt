package io.github.reactivecircus.kstreamlined.backend.datafetcher.mapper

import io.github.reactivecircus.kstreamlined.backend.client.dto.KotlinWeeklyItem
import io.github.reactivecircus.kstreamlined.backend.client.dto.Link
import io.github.reactivecircus.kstreamlined.backend.client.dto.TalkingKotlinItem
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.KotlinWeekly
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.TalkingKotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class KotlinWeeklyEntryMapperTest {

    @Test
    fun `toKotlinWeeklyEntry() converts KotlinWeeklyItem to KotlinWeekly`() {
        val expected = KotlinWeekly(
            id = "21a2c7f9e24fae1631468c5507e4ff7c",
            title = "@KotlinWeekly: Kotlin Weekly #312 has just been published!",
            publishDate = "Sun, 24 Jul 2022 15:03:40 GMT",
            contentUrl = "https://twitter.com/KotlinWeekly/status/1551221582248419328",
            newsletterLogoUrl = "logo-url",
        )
        val actual = KotlinWeeklyItem(
            title = " @KotlinWeekly: Kotlin Weekly #312 has just been published! - ",
            description = " <blockquote class=\"twitter-tweet\" data-width=\"550\"><p lang=\"en\" dir=\"ltr\">Kotlin Weekly #312 has just been published! - <a href=\"https://t.co/7JzvarYb05\">https://t.co/7JzvarYb05</a></p>— Kotlin Weekly (@KotlinWeekly) <a href=\"https://twitter.com/KotlinWeekly/status/1551221582248419328?ref_src=twsrc%5Etfw\">July 24, 2022</a></blockquote> <script async src=\"https://platform.twitter.com/widgets.js\" charset=\"utf-8\"></script> ",
            link = "https://twitter.com/KotlinWeekly/status/1551221582248419328",
            guid = "21a2c7f9e24fae1631468c5507e4ff7c",
            creator = " @KotlinWeekly ",
            pubDate = "Sun, 24 Jul 2022 15:03:40 GMT",
        ).toKotlinWeeklyEntry(logoUrl = "logo-url")

        assertEquals(expected, actual)
    }
}
