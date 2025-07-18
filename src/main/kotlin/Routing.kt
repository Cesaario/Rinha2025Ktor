package app.cesario

import app.cesario.dto.PaymentRequest
import app.cesario.services.PaymentService
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val apiService = PaymentService()

    routing {
        post("/payments") {
            val request = call.receive<PaymentRequest>()
            val response = apiService.processPayment(request)
            call.respond(response)
        }
    }
}
