package app.cesario

import app.cesario.dto.PaymentRequest
import app.cesario.services.QueueService
import app.cesario.services.RedisService
import app.cesario.services.SummaryService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime

fun Application.configureRouting() {
    routing {
        post("/payments") {
            try {
                val request = call.receive<PaymentRequest>()
                queueScope.launch {
                    QueueService.addPaymentRequestToQueue(request)
                }
                call.respond(HttpStatusCode.Accepted)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, "Error processing payment request: ${e.message}")
            }
        }
        get("/payments-summary") {
            val from = call.parameters["from"]
            val to = call.parameters["to"]
            val start = if (from != null) OffsetDateTime.parse(from) else null
            val end = if (to != null) OffsetDateTime.parse(to) else null
            val summary = SummaryService.getPaymentSummary(start, end)
            call.respond(summary)
        }
        delete("/payments") {
            RedisService.resetPayments()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
