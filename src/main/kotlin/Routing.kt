package app.cesario

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    val apiService = ExampleService()

    routing {
        post("/payments") {
            val todo = apiService.getTodo()
            call.respond(todo)
        }
    }
}
