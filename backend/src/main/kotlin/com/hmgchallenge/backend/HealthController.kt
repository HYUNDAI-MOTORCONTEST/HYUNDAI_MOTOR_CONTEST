package com.hmgchallenge.backend

import com.hmgchallenge.shared.HealthResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class HealthController(
    @Value("\${spring.application.name}") private val serviceName: String,
    @Value("\${app.version:0.0.1}") private val version: String,
) {
    @GetMapping(HealthResponse.PATH)
    fun health(): HealthResponse = HealthResponse(
        status = HealthResponse.STATUS_UP,
        service = serviceName,
        version = version,
        timestamp = Instant.now().toString(),
    )
}
