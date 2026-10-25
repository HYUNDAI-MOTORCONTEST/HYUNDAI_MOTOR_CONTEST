package com.hmgchallenge.shared

import kotlinx.serialization.Serializable

/**
 * `GET /api/health` 응답 DTO. 백엔드(Jackson)와 앱(kotlinx.serialization)이 함께 쓴다.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val version: String,
    val timestamp: String,
) {
    companion object {
        const val PATH = "/api/health"
        const val STATUS_UP = "UP"
    }
}
