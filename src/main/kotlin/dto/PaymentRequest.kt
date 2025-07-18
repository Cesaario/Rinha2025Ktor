package app.cesario.dto

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class PaymentRequest(
    val correlationId: String,
    val amount: Double
)