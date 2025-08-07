package app.cesario.services

import app.cesario.dto.PaymentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.timerTask

object PaymentRouterService {
    val log = LoggerFactory.getLogger(this::class.java)

    const val SERVICE_RESOLVER_INTERVAL = 4000L

    // If the response time from the default is higher than the response time from the fallback by this factor,
    // then the fallback will result in a better amount ratio.
    // const val DECISION_FACTOR = 1.11
    const val DECISION_FACTOR = 2.0

    var serviceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT

    suspend fun processPayment(paymentRequest: PaymentRequest) {
        val response = PaymentService.processPayment(paymentRequest, serviceToBeUsed)
        // log.info("id: ${paymentRequest.correlationId}, service: $serviceToBeUsed, response: ${response?.success}, responseTime: ${response?.responseTime}ms")
        if (response?.success == true) {
            SummaryService.registerProcessedPayment(response)
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

        if (defaultHealthStatus == null || fallbackHealthStatus == null)
            return

        var newServiceToBeUsed: PaymentService.PaymentProcessorService = if (defaultHealthStatus.failing && !fallbackHealthStatus.failing) {
            PaymentService.PaymentProcessorService.FALLBACK
        } else if (!defaultHealthStatus.failing && fallbackHealthStatus.failing) {
            PaymentService.PaymentProcessorService.DEFAULT
        } else {
            if (defaultHealthStatus.minResponseTime <= fallbackHealthStatus.minResponseTime * DECISION_FACTOR)
                PaymentService.PaymentProcessorService.DEFAULT
            else
                PaymentService.PaymentProcessorService.FALLBACK
        }

        newServiceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT

        if (serviceToBeUsed != newServiceToBeUsed) {
            log.info("Switching service from ${serviceToBeUsed.name} to ${newServiceToBeUsed.name}")
            log.info(
                "Response times: default=${defaultHealthStatus.minResponseTime}, fallback=${fallbackHealthStatus.minResponseTime}"
            )
            log.info("Default failing: ${defaultHealthStatus.failing}, Fallback failing: ${fallbackHealthStatus.failing}")
            serviceToBeUsed = newServiceToBeUsed
        }
    }
}