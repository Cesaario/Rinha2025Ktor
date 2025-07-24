package app.cesario.services

import app.cesario.dto.PaymentSummaryForService
import app.cesario.dto.ProcessedPayment
import java.time.OffsetDateTime

object SummaryService {
    fun registerProcessedPayment(processedPayment: ProcessedPayment){
        RedisService.savePaymentToSortedSet(processedPayment)
    }

    suspend fun getPaymentSummary(start: OffsetDateTime?, end: OffsetDateTime?): Map<String, PaymentSummaryForService> {
        val payments = RedisService.getPaymentsFromSortedSet(start, end)
        val defaultSummary = PaymentSummaryForService()
        val fallbackSummary = PaymentSummaryForService()
        payments.forEach {
            if(it.service == PaymentService.PaymentProcessorService.DEFAULT) {
                defaultSummary.totalRequests++
                defaultSummary.totalAmount += it.amount
            } else if (it.service == PaymentService.PaymentProcessorService.FALLBACK) {
                fallbackSummary.totalRequests++
                fallbackSummary.totalAmount += it.amount
            }
        }

        return mapOf(
            PaymentService.PaymentProcessorService.DEFAULT.name.lowercase() to defaultSummary,
            PaymentService.PaymentProcessorService.FALLBACK.name.lowercase() to fallbackSummary
        )
    }
}