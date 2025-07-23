package app.cesario.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProcessedPayment(
    val correlationId: String,
    val amount: Double,
    val requestedAt: String,
)
