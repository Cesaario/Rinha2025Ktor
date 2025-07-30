package app.cesario

import app.cesario.services.PaymentRouterService
import app.cesario.services.PaymentService
import app.cesario.services.QueueService
import app.cesario.services.QueueService.requestQueue
import app.cesario.services.RedisService
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    log.info("Starting Ktor application :D")

    val processorJob = SupervisorJob()
    val processorScope = CoroutineScope(Dispatchers.IO + processorJob)

    monitor.subscribe(ApplicationStarted) {
        // Launch multiple consumers for parallel processing - optimized for resource constraints
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
