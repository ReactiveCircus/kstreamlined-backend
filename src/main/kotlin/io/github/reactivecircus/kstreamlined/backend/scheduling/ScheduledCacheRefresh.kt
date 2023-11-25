package io.github.reactivecircus.kstreamlined.backend.scheduling

import io.github.reactivecircus.kstreamlined.backend.datafetcher.FeedEntryDataFetcher
import io.github.reactivecircus.kstreamlined.backend.schema.generated.types.FeedSourceKey
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class ScheduledCacheRefresh {

    @Autowired
    private lateinit var feedEntryDataFetcher: FeedEntryDataFetcher

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    fun refreshFeedEntryCache() = runBlocking {
        feedEntryDataFetcher.feedEntries(FeedSourceKey.entries)
    }
}
