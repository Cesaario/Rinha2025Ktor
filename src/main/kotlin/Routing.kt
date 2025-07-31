package app.cesario

import app.cesario.dto.PaymentRequest
import app.cesario.services.PaymentService
import app.cesario.services.QueueService
import app.cesario.services.RedisService
import app.cesario.services.SummaryService
import io.ktor.client.plugins.HttpCallValidator
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime

fun Application.configureRouting() {
    val log = LoggerFactory.getLogger(this::class.java)

    routing {
        post("/payments") {
            try {
                val start = System.nanoTime()

                val request = call.receive<PaymentRequest>()
                val endSerialize = System.nanoTime()

                QueueService.addPaymentRequestToQueue(request)
                val endQueue = System.nanoTime()

                call.respond(HttpStatusCode.Accepted)
                val endRespose = System.nanoTime()

                // log.info("serialize: ${start - endSerialize} ns, queue: ${endSerialize - endQueue} ns, response: ${endQueue - endRespose} ns")

            } catch (e: Exception) {
                log.error("Error processing payment request: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing payment request: ${e.message}")
            }
        }
        get("/payments-summary") {
            val from = call.parameters["from"]
            val to = call.parameters["to"]
            val start = if(from != null) OffsetDateTime.parse(from) else null
            val end = if(to != null) OffsetDateTime.parse(to) else null
            val summary = SummaryService.getPaymentSummary(start, end)
            call.respond(summary)
        }
        delete("/payments") {
            RedisService.resetPayments()
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
