package app.cesario.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentSummaryForService(
    var totalRequests: Int = 0,
    var totalAmount: Double = 0.0
)