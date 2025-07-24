package app.cesario

import app.cesario.services.PaymentRouterService
import app.cesario.services.PaymentService
import app.cesario.services.RedisService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("Starting Ktor application :D")

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }

    monitor.subscribe(ApplicationStopped) {
        RedisService.closeConnection()
    }

    configureRouting()

    monitor.subscribe(ApplicationStarted) {
        RedisService.startRequestConsumer()
    }
}
