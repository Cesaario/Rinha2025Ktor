package app.cesario.dto

import app.cesario.dto.serializer.BigDecimalSerializer
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class PaymentSummaryForService(
    var totalRequests: Int = 0,
    @Serializable(with = BigDecimalSerializer::class)
    var totalAmount: BigDecimal = BigDecimal.ZERO
)