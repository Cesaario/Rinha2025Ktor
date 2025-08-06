package app.cesario.dto

import app.cesario.dto.serializer.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PaymentProcessRequest(
    val correlationId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val requestedAt: String
)
