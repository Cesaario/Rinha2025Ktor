package app.cesario.services

import app.cesario.dto.PaymentRequest
import org.slf4j.LoggerFactory

/**
 * Ideia: e se ao invés de chamar o endpoint de status dos processors, por que eu não me baseio no tempo de resposta de cada requisição para eles?
 * Assim, tenho o tempo de resposta em tempo real.
 */
object PaymentRouterService {
    private val log = LoggerFactory.getLogger(this::class.java)

    suspend fun processPayment(paymentRequest: PaymentRequest) {
        log.info("Processing payment request: $paymentRequest")
        val response = PaymentService.processPayment(paymentRequest, PaymentService.PaymentProcessorService.DEFAULT)
        if (response != null) {
            SummaryService.registerProcessedPayment(response)
            log.info("Payment request processed! (${response.correlationId})")
        } else {
            log.info("Error processing payment request: ${paymentRequest.correlationId}")
        }
    }
}