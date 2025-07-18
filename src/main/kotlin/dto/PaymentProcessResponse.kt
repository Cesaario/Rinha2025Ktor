package app.cesario.dto

import kotlinx.serialization.Serializable

@Serializable
data class PaymentProcessResponse(
    val message: String
)