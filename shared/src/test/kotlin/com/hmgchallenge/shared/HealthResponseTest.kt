package com.hmgchallenge.shared

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class HealthResponseTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun roundTrip() {
        val original = HealthResponse(HealthResponse.STATUS_UP, "backend", "0.0.1", "2026-10-25T00:00:00Z")
        val decoded = json.decodeFromString<HealthResponse>(json.encodeToString(HealthResponse.serializer(), original))
        assertEquals(original, decoded)
    }

    @Test
    fun ignoresUnknownFields() {
        val decoded = json.decodeFromString<HealthResponse>(
            """{"status":"UP","service":"backend","version":"0.0.1","timestamp":"t","extra":1}""",
        )
        assertEquals("UP", decoded.status)
    }
}
