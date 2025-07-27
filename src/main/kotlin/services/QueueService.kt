package app.cesario.services

import app.cesario.dto.PaymentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object QueueService {
    private val log = LoggerFactory.getLogger(this::class.java)

    val requestQueue = Channel<PaymentRequest>(capacity = Channel.UNLIMITED)

    fun addPaymentRequestToQueue(paymentRequest: PaymentRequest) {
        CoroutineScope(Dispatchers.IO).launch {
            requestQueue.send(paymentRequest)
        }
    }

    fun initializeConsumer() {
        CoroutineScope(Dispatchers.IO).launch {

        }
    }
}