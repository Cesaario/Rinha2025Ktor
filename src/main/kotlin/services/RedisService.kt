package app.cesario.services

import app.cesario.dto.PaymentRequest
import io.lettuce.core.ClientOptions
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.RedisAsyncCommandsImpl
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.api.async.RedisAsyncCommands
import io.lettuce.core.api.coroutines
import io.lettuce.core.api.sync.RedisCommands
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors

@OptIn(ExperimentalLettuceCoroutinesApi::class)
class RedisService(val paymentRouterService: PaymentRouterService) {
    private val log = LoggerFactory.getLogger(this::class.java)

    val queue = "payment_requests"

    private val redisURI = RedisURI.Builder.redis("localhost", 6379)
        .withTimeout(Duration.ofMinutes(10))
        .build()
    private val client = RedisClient.create(redisURI)
    private val listenerConnection = client.connect()
    private val producerConnection = client.connect()
    private val listenerCommands = listenerConnection.async()
    private val producerCommands = producerConnection.async()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val json = Json

    fun addPaymentRequestToQueue(paymentRequest: PaymentRequest) {
        try {
            log.info("Adding payment request to queue: $paymentRequest")
            producerCommands.lpush(queue, json.encodeToString(paymentRequest))
            log.info("Payment request added to queue successfully")
        } catch (e: Exception) {
            log.error("Error adding payment request to queue: ${e.message}", e)
            throw e
        }

    }

    fun startRequestConsumer() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val result = listenerCommands.brpop(0, queue)
                    log.info("Request received from queue: $result")
                    if (result != null) {
                        val request = json.decodeFromString<PaymentRequest>(result.await().value)
                        paymentRouterService.processPayment(request)
                    }
                } catch (e: Exception) {
                    log.error("Error processing payment request: ${e.message}", e)
                }
            }
        }
    }

    fun closeConnection() {
        try {
            log.info("Closing Redis connection")
            producerConnection.close()
            listenerConnection.close()
            client.shutdown()
            log.info("Redis connection closed successfully")
        } catch (e: Exception) {
            log.error("Error closing Redis connection: ${e.message}", e)
        }
    }

}