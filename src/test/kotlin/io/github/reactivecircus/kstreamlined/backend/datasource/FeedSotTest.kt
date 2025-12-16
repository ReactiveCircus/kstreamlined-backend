package io.github.reactivecircus.kstreamlined.backend.datasource

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FeedSotTest {
    @Test
    fun `feedSot returns persisted remote data from local source`() = runBlocking {
        val remoteData = listOf("item1", "item2")
        val localData = mutableListOf("item0")

        val result = feedSot(
            localSource = { localData },
            persistToLocal = { newData ->
                localData.addAll(newData)
            },
            remoteSource = { remoteData },
        )

        assertEquals(listOf("item0", "item1", "item2"), result)
    }

    @Test
    fun `feedSot throws exception when local source is null`() = runBlocking {
        val exception = assertFailsWith<IllegalStateException> {
            feedSot(
                localSource = { null },
                persistToLocal = {},
                remoteSource = { emptyList() },
            )
        }
        assertEquals("Local source returned null.", exception.message)
    }
}
