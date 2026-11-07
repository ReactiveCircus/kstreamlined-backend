package io.github.reactivecircus.kstreamlined.backend.service

import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

val NoOpRedisClient = RedisClient(
    engine = MockEngine { request ->
        when {
            request.url.encodedPath.contains("/get/") -> respond(content = "{ \"result\": null }")
            request.url.encodedPath.endsWith("/multi-exec") -> respond(content = "[{ \"result\": \"OK\" }, { \"result\": \"OK\" }]")
            else -> respond(content = "{ \"result\": 0 }")
        }
    },
    url = "",
    token = "",
)
