package app.cesario.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServiceHealthStatus(
    val failing: Boolean,
    val minResponseTime: Int
)
