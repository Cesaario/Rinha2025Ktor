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
import io.ktor.server.config.ApplicationConfig
import io.ktor.util.collections.asBoolean
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

    private val servicesUrl = mutableMapOf<PaymentProcessorService, String>()

    const val SERVICE_HEALTH_CHECK_INTERVAL = 5000L

    var isUpdater = false

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
            })
        }
    }

    fun init(config: ApplicationConfig) {
        val defaultServiceHost = config.propertyOrNull("ktor.services.default_processor.host")?.getString()!!
        val defaultServicePort = config.propertyOrNull("ktor.services.default_processor.port")?.getString()!!
        val fallbackServiceHost = config.propertyOrNull("ktor.services.fallback_processor.host")?.getString()!!
        val fallbackServicePort = config.propertyOrNull("ktor.services.fallback_processor.port")?.getString()!!
        servicesUrl.put(PaymentProcessorService.DEFAULT, "http://$defaultServiceHost:$defaultServicePort")
        servicesUrl.put(PaymentProcessorService.FALLBACK, "http://$fallbackServiceHost:$fallbackServicePort")
        isUpdater = config.propertyOrNull("ktor.deployment.healthCheckUpdater")?.getString().toBoolean()

        log.info("Default: ${servicesUrl[PaymentProcessorService.DEFAULT]}, Fallback: ${servicesUrl[PaymentProcessorService.FALLBACK]}")
    }

    suspend fun processPayment(paymentRequest: PaymentRequest, service: PaymentProcessorService): ProcessedPayment? {
        val now = OffsetDateTime.now()
        val payload =
            PaymentProcessRequest(paymentRequest.correlationId, paymentRequest.amount, now.toString())

        try {
            val before = System.currentTimeMillis()
            val response = client.post("${service.getUrl()}/payments") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }
            val after = System.currentTimeMillis()
            val responseTime = after - before

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

    fun startHealthCheckFetcherInterval() {
        Timer().scheduleAtFixedRate(
            timerTask {
                CoroutineScope(Dispatchers.IO).launch {
                    updateServiceHealthCheck()
                }
            },
            1000L,
            SERVICE_HEALTH_CHECK_INTERVAL
        )
    }

    suspend fun updateServiceHealthCheck() {
        var shouldUpdate = false

        if(!isUpdater) {
            PaymentRouterService.updateServiceToBeUsed()
            return
        }

        PaymentProcessorService.entries.forEach {
            try {
                val url = it.getUrl()
                if (url == null)
                    return@forEach
                val result = client.get("${url}/payments/service-health") {
                    contentType(ContentType.Application.Json)
                }.body<ServiceHealthStatus>()
                RedisService.updateProcessorHealthStatus(result, it)
                shouldUpdate = true
            } catch (e: Exception) {
                log.error("Error updating health status for ${it.name}: ${e.message}", e)
            }
        }
        if(shouldUpdate)
            PaymentRouterService.updateServiceToBeUsed()
    }

    enum class PaymentProcessorService {
        DEFAULT,
        FALLBACK;

        fun getUrl(): String? {
            return "${servicesUrl[this]}"
        }
    }
}