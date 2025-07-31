package app.cesario.dto

import app.cesario.dto.serializer.BigDecimalSerializer
import app.cesario.services.PaymentService
import kotlinx.serialization.Serializable
import java.math.BigDecimal

@Serializable
data class ProcessedPayment(
    val correlationId: String,
    @Serializable(with = BigDecimalSerializer::class)
    val amount: BigDecimal,
    val timestamp: Long,
    val service: PaymentService.PaymentProcessorService,
    val responseTime: Long,
    val success: Boolean
)
