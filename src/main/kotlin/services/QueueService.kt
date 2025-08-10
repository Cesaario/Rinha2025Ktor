package app.cesario.services

import app.cesario.dto.PaymentRequest
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel

object QueueService {
    val requestQueue = Channel<PaymentRequest>(capacity = Channel.UNLIMITED, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    suspend fun addPaymentRequestToQueue(paymentRequest: PaymentRequest) {
        requestQueue.send(paymentRequest)
    }
}