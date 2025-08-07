package app.cesario.services

import app.cesario.dto.PaymentRequest
import kotlinx.coroutines.channels.Channel

object QueueService {
    val requestQueue = Channel<PaymentRequest>(capacity = Channel.UNLIMITED)

    suspend fun addPaymentRequestToQueue(paymentRequest: PaymentRequest) {
        requestQueue.send(paymentRequest)
    }
}