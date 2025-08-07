package app.cesario

import app.cesario.services.PaymentRouterService
import app.cesario.services.PaymentService
import app.cesario.services.QueueService.requestQueue
import app.cesario.services.RedisService
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors

val processorJob = SupervisorJob()
val processorScope = CoroutineScope(Dispatchers.IO + processorJob)
val queueScope = CoroutineScope(Executors.newFixedThreadPool(8).asCoroutineDispatcher() + processorJob)

fun main(args: Array<String>) {
    embeddedServer(CIO, port = 9999, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    log.info("Starting Native Ktor application :D")

    monitor.subscribe(ApplicationStarted) {
        repeat(10) {
            processorScope.launch {
                for (request in requestQueue) {
                    PaymentRouterService.processPayment(request)
                }
            }
        }
    }

    RedisService.init(environment.config)
    PaymentService.init(environment.config)

    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            encodeDefaults = true
        })
    }

    monitor.subscribe(ApplicationStopped) {
        RedisService.closeConnection()
        processorJob.cancel()
    }

    configureRouting()

    PaymentService.startHealthCheckFetcherInterval()
    PaymentRouterService.startServiceResolverInterval()
}
