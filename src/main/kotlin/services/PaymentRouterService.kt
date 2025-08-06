package app.cesario.services

import app.cesario.dto.PaymentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.timerTask

object PaymentRouterService {
    const val SERVICE_RESOLVER_INTERVAL = 5500L

    // If the response time from the default is higher than the response time from the fallback by this factor,
    // then the fallback will result in a better amount ratio.
    // const val DECISION_FACTOR = 1.11
    const val DECISION_FACTOR = 2.0

    var serviceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT

    suspend fun processPayment(paymentRequest: PaymentRequest) {
        val response = PaymentService.processPayment(paymentRequest, serviceToBeUsed)
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

        if (defaultHealthStatus.failing && !fallbackHealthStatus.failing) {
            serviceToBeUsed = PaymentService.PaymentProcessorService.FALLBACK
            return
        }

        if (!defaultHealthStatus.failing && fallbackHealthStatus.failing) {
            serviceToBeUsed = PaymentService.PaymentProcessorService.DEFAULT
            return
        }

        if (defaultHealthStatus.failing && fallbackHealthStatus.failing)
            return

        serviceToBeUsed =
            if (defaultHealthStatus.minResponseTime < 30 || defaultHealthStatus.minResponseTime <= fallbackHealthStatus.minResponseTime * DECISION_FACTOR)
                PaymentService.PaymentProcessorService.DEFAULT
            else
                PaymentService.PaymentProcessorService.FALLBACK
    }
}