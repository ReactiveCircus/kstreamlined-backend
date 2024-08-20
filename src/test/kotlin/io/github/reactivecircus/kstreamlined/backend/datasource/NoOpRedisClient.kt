package io.github.reactivecircus.kstreamlined.backend.datasource

import io.github.reactivecircus.kstreamlined.backend.redis.RedisClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond

val NoOpRedisClient = RedisClient(
    engine = MockEngine { request ->
        if (request.url.encodedPath.contains("get")) {
            respond(content = "{ \"result\": null }")
        } else {
            respond(content = "{ \"result\": \"OK\" }")
        }
    },
    url = "",
    token = "",
)
