package io.github.reactivecircus.kstreamlined.backend.integration

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EmulatorEndpointTest {
    @ParameterizedTest
    @ValueSource(strings = ["127.0.0.1:8681", "localhost:8681"])
    fun `explicit loopback endpoints are accepted`(host: String) {
        assertEquals(host, requireEmulatorHost(host))
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
        strings = [
            "example.invalid:8681",
            "0.0.0.0:8681",
            "http://127.0.0.1:8681",
            "127.0.0.1",
            "127.0.0.1:0",
            "127.0.0.1:65536",
            "127.0.0.1:999999999999999999999",
        ],
    )
    fun `missing non-loopback and malformed endpoints fail before connecting`(host: String?) {
        assertFailsWith<IllegalArgumentException> { requireEmulatorHost(host) }
    }
}
