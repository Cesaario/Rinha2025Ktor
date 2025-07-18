package app.cesario.dto

import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.util.UUID

@Serializable
data class PaymentProcessRequest(
    val correlationId: String,
    val amount: Double,
    val requestedAt: String
)
