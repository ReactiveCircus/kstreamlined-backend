package io.github.reactivecircus.kstreamlined.backend.datasource

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.serialization.kotlinx.json.DefaultJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class DataLoader<T : Any> private constructor(
    private val localCache: Cache<String, List<T>>,
    private val redisClient: RedisClient,
    private val remoteCacheExpiry: Duration,
    private val listSerializer: KSerializer<List<T>>,
) {
    class CacheConfig(
        val localExpiry: Duration,
        val remoteExpiry: Duration,
    )

    @Suppress("ReturnCount")
    suspend fun load(
        key: String,
        sotOnly: Boolean = false,
        sot: suspend () -> List<T>
    ): List<T> {
        if (sotOnly) return loadFromSot(key, sot)

        // L1 cache - local
        val l1Value = localCache.getIfPresent(key)
        if (l1Value != null) {
            return l1Value
        }

        // L2 cache - remote (Redis)
        val l2Value = redisClient.get(key)
        if (l2Value != null) {
            val value = DefaultJson.decodeFromString(listSerializer, l2Value)
            localCache.put(key, value)
            return value
        }

        // Source of truth
        return loadFromSot(key, sot)
    }

    private suspend fun loadFromSot(key: String, sot: suspend () -> List<T>): List<T> {
        val sotValue = sot()
        localCache.put(key, sotValue)
        redisClient.set(
            key = key,
            value = DefaultJson.encodeToString(listSerializer, sotValue),
            keyExpirySeconds = remoteCacheExpiry.inWholeSeconds.toInt(),
        )
        return sotValue
    }

    companion object {
        fun <T : Any> of(
            cacheConfig: CacheConfig,
            redisClient: RedisClient,
            serializer: KSerializer<T>,
        ): DataLoader<T> {
            val localCache = Caffeine
                .newBuilder()
                .expireAfterWrite(cacheConfig.localExpiry.toJavaDuration())
                .build<String, List<T>>()
            return DataLoader(
                localCache = localCache,
                redisClient = redisClient,
                remoteCacheExpiry = cacheConfig.remoteExpiry,
                listSerializer = ListSerializer(serializer),
            )
        }

        fun <T : Any> of(
            localCache: Cache<String, List<T>>,
            remoteCacheExpiry: Duration,
            redisClient: RedisClient,
            serializer: KSerializer<T>,
        ): DataLoader<T> {
            return DataLoader(
                localCache = localCache,
                redisClient = redisClient,
                remoteCacheExpiry = remoteCacheExpiry,
                listSerializer = ListSerializer(serializer),
            )
        }
    }
}
