package app.cesario.services

import app.cesario.dto.PaymentRequest
import app.cesario.services.PaymentService.updateServiceHealthCheck
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.Timer
import kotlin.concurrent.timerTask

/**
 * Ideia: e se ao invés de chamar o endpoint de status dos processors, por que eu não me baseio no tempo de resposta de cada requisição para eles?
 * Assim, tenho o tempo de resposta em tempo real.
 */
object PaymentRouterService {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val SERVICE_RESOLVER_INTERVAL = 5000L

    // If the response time from the default is higher than the response time from the fallback by this factor,
    // then the fallback will result in a better amount ratio.
    const val DECISION_FACTOR = 1.11

    var serviceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT

    suspend fun processPayment(paymentRequest: PaymentRequest) {
        //log.info("Processing payment request: $paymentRequest")
        val response = PaymentService.processPayment(paymentRequest, serviceToBeUsed)
        if (response?.success == true) {
            SummaryService.registerProcessedPayment(response)
        } else {
            // Requeue payment request if processing failed
            // RedisService.addPaymentRequestToQueue(paymentRequest)
        }
    }

    fun startServiceResolverInterval() {
        Timer().scheduleAtFixedRate(
            timerTask {
                CoroutineScope(Dispatchers.IO).launch {
                    updateServiceToBeUsed()
                }
            },
            0L,
            SERVICE_RESOLVER_INTERVAL
        )
    }


    suspend fun updateServiceToBeUsed() {
        val defaultHealthStatus = RedisService.getServiceHealthStatus(PaymentService.PaymentProcessorService.DEFAULT)
        val fallbackHealthStatus = RedisService.getServiceHealthStatus(PaymentService.PaymentProcessorService.FALLBACK)

        if(defaultHealthStatus == null || fallbackHealthStatus == null)
            return

        if (defaultHealthStatus.failing && !fallbackHealthStatus.failing) {
            serviceToBeUsed = PaymentService.PaymentProcessorService.FALLBACK
            // log.info("Default service is failing, using fallback service")
            return
        }
        if (!defaultHealthStatus.failing && fallbackHealthStatus.failing) {
            serviceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT
            // log.info("Fallback service is failing, using default service")
            return
        }
        if (defaultHealthStatus.failing && fallbackHealthStatus.failing) {
            // log.info("Both services are failing, keeping the current service: ${serviceToBeUsed.name}")
            return
        }

        serviceToBeUsed =
            if (defaultHealthStatus.minResponseTime <= fallbackHealthStatus.minResponseTime * DECISION_FACTOR)
                PaymentService.PaymentProcessorService.DEFAULT
            else
                PaymentService.PaymentProcessorService.FALLBACK

//        log.info(
//            "Using service ${serviceToBeUsed.name} based on response times: default=${defaultHealthStatus.minResponseTime}, fallback=${fallbackHealthStatus.minResponseTime}"
//        )
    }
}