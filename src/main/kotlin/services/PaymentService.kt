package app.cesario.services

import app.cesario.dto.PaymentProcessRequest
import app.cesario.dto.PaymentProcessResponse
import app.cesario.dto.PaymentRequest
import app.cesario.dto.ProcessedPayment
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.time.OffsetDateTime

class PaymentService {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                isLenient = true
            })
        }
    }

    suspend fun processPayment(paymentRequest: PaymentRequest, service: PaymentProcessorService): ProcessedPayment? {
        val payload =
            PaymentProcessRequest(paymentRequest.correlationId, paymentRequest.amount, OffsetDateTime.now().toString())

        try {
            client.post("${service.url}/payments") {
                contentType(ContentType.Application.Json)
                setBody(payload)
            }

            return ProcessedPayment(
                payload.correlationId,
                payload.amount,
                payload.requestedAt
            )
        } catch (e: Exception) {
            return null
        }
    }

    enum class PaymentProcessorService {
        MAIN("http://localhost:8001"),
        FALLBACK("http://localhost:8002");

        val url: String

        constructor(url: String) {
            this.url = url
        }
    }
}