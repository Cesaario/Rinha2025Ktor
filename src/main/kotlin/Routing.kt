package app.cesario

import app.cesario.dto.PaymentRequest
import app.cesario.services.PaymentService
import app.cesario.services.RedisService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun Application.configureRouting(redisService: RedisService) {
    val log = LoggerFactory.getLogger(this::class.java)

    routing {
        post("/payments") {
            try {
                val request = call.receive<PaymentRequest>()
                redisService.addPaymentRequestToQueue(request)
                call.respond(HttpStatusCode.Accepted)
            } catch (e: Exception) {
                log.error("Error processing payment request: ${e.message}", e)
                call.respond(HttpStatusCode.InternalServerError, "Error processing payment request: ${e.message}")
            }
        }
    }
}
