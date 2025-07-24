package app.cesario.dto

import app.cesario.services.PaymentService
import kotlinx.serialization.Serializable

@Serializable
data class ProcessedPayment(
    val correlationId: String,
    val amount: Double,
    val timestamp: Long,
    val service: PaymentService.PaymentProcessorService
)
