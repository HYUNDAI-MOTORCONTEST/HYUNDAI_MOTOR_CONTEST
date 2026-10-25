package com.hmgchallenge.backend

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class HealthControllerTest {
    @Test
    fun returnsUp() {
        val response = HealthController(serviceName = "hmg-backend", version = "test").health()
        assertEquals("UP", response.status)
        assertEquals("hmg-backend", response.service)
    }
}
