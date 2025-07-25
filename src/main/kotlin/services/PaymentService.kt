package app.cesario.services

import app.cesario.dto.PaymentProcessRequest
import app.cesario.dto.PaymentRequest
import app.cesario.dto.ProcessedPayment
import app.cesario.dto.ServiceHealthStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.Timer
import kotlin.concurrent.timerTask

object PaymentService {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val SERVICE_HEALTH_CHECK_INTERVAL = 5000L

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
            })
        }
    }

    suspend fun processPayment(paymentRequest: PaymentRequest, service: PaymentProcessorService): ProcessedPayment? {
        val now = OffsetDateTime.now()
        val payload =
            PaymentProcessRequest(paymentRequest.correlationId, paymentRequest.amount, now.toString())

        try {
            val before = System.currentTimeMillis()
            val response = client.post("${service.url}/payments") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val after = System.currentTimeMillis()
            val responseTime = after - before

            // log.info("Payment request ${paymentRequest.correlationId} processed!")

            log.info("Service=${service.name}, responseTime=${responseTime}, status=${response.status}")

            return ProcessedPayment(
                payload.correlationId,
                payload.amount,
                now.toInstant().toEpochMilli(),
                service,
                responseTime,
                response.status == HttpStatusCode.OK
            )
        } catch (e: Exception) {
            log.error("Error processing payment request: ${e.message}", e)
            return null
        }
    }

    fun startHealthCheckInterval() {
        Timer().scheduleAtFixedRate(
            timerTask {
                CoroutineScope(Dispatchers.IO).launch {
                    updateServiceHealthCheck()
                }
            },
            0L,
            SERVICE_HEALTH_CHECK_INTERVAL
        )
    }

    suspend fun updateServiceHealthCheck() {
        PaymentProcessorService.entries.forEach {
            try {
                val result = client.get("${it.url}/payments/service-health") {
                    contentType(ContentType.Application.Json)
                }.body<ServiceHealthStatus>()
                RedisService.updateProcessorHealthStatus(result, it)
            } catch (e: Exception) {
                log.error("Error updating health status for ${it.name}: ${e.message}", e)
            }
        }
        PaymentRouterService.updateServiceToBeUsed()
    }

    enum class PaymentProcessorService {
        DEFAULT("http://localhost:8001"),
        FALLBACK("http://localhost:8002");

        val url: String

        constructor(url: String) {
            this.url = url
        }
    }
}