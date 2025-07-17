package app.cesario

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing {
        post("/payments") {
            call.respondText("Hello World!")
        }
    }
}
