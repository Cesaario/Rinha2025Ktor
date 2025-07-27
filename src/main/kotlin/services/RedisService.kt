package app.cesario.services

import app.cesario.dto.PaymentRequest
import app.cesario.dto.ProcessedPayment
import app.cesario.dto.ServiceHealthStatus
import io.ktor.server.config.ApplicationConfig
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.Range
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.OffsetDateTime

@OptIn(ExperimentalLettuceCoroutinesApi::class)
object RedisService {
    private val log = LoggerFactory.getLogger(this::class.java)

    const val QUEUE_KEY = "payment_requests"
    const val PROCESSED_PAYMENTS_SET_KEY = "processed_payments"
    const val DEFAULT_PROCESSOR_RESPONSE_TIME_KEY = "default_processor_response_time"
    const val DEFAULT_PROCESSOR_FAILING_KEY = "default_processor_failing"
    const val FALLBACK_PROCESSOR_RESPONSE_TIME_KEY = "fallback_processor_response_time"
    const val FALLBACK_PROCESSOR_FAILING_KEY = "fallback_processor_failing"

    private lateinit var redisURI: RedisURI
    private lateinit var client: RedisClient
    private lateinit var listenerConnection: io.lettuce.core.api.StatefulRedisConnection<String, String>
    private lateinit var producerConnection: io.lettuce.core.api.StatefulRedisConnection<String, String>
    private lateinit var listenerCommands: io.lettuce.core.api.async.RedisAsyncCommands<String, String>
    private lateinit var producerCommands: io.lettuce.core.api.async.RedisAsyncCommands<String, String>
    private var initialized = false

    fun init(config: ApplicationConfig) {
        val redisHost = config.propertyOrNull("ktor.services.redis.host")?.getString()
        val redisPort = config.propertyOrNull("ktor.services.redis.port")?.getString()!!.toInt()

        log.info("Redis host: $redisHost, port: $redisPort")

        redisURI = RedisURI.Builder.redis(redisHost, redisPort)
            .withTimeout(Duration.ofMinutes(10))
            .build()
        client = RedisClient.create(redisURI)
        listenerConnection = client.connect()
        producerConnection = client.connect()
        listenerCommands = listenerConnection.async()
        producerCommands = producerConnection.async()

        startRequestConsumer()
        initialized = true
    }

    fun savePaymentToSortedSet(processedPayment: ProcessedPayment) {
        val score = processedPayment.timestamp.toDouble()
        val json = Json.encodeToString(processedPayment)
        producerCommands.zadd(PROCESSED_PAYMENTS_SET_KEY, score, json)
    }

    suspend fun getPaymentsFromSortedSet(start: OffsetDateTime?, end: OffsetDateTime?): List<ProcessedPayment> {
        val startScore = start?.toInstant()?.toEpochMilli()?.toDouble()
        val endScore = end?.toInstant()?.toEpochMilli()?.toDouble()

        val range = when {
            startScore != null && endScore != null -> Range.create(startScore, endScore)
            startScore != null -> Range.create(startScore, Double.MAX_VALUE)
            endScore != null -> Range.create(Double.MIN_VALUE, endScore)
            else -> Range.create(Double.MIN_VALUE, Double.MAX_VALUE)
        }

        val results = producerCommands.zrangebyscore(PROCESSED_PAYMENTS_SET_KEY, range).await()

        return results.mapNotNull { runCatching { Json.decodeFromString<ProcessedPayment>(it) }.getOrNull() }
    }

    suspend fun getServiceHealthStatus(service: PaymentService.PaymentProcessorService): ServiceHealthStatus? {
        if(!initialized)
            return null

        val responseTimeKey = when (service) {
            PaymentService.PaymentProcessorService.DEFAULT -> DEFAULT_PROCESSOR_RESPONSE_TIME_KEY
            PaymentService.PaymentProcessorService.FALLBACK -> FALLBACK_PROCESSOR_RESPONSE_TIME_KEY
        }
        val failingKey = when (service) {
            PaymentService.PaymentProcessorService.DEFAULT -> DEFAULT_PROCESSOR_FAILING_KEY
            PaymentService.PaymentProcessorService.FALLBACK -> FALLBACK_PROCESSOR_FAILING_KEY
        }

        val responseTime = producerCommands.get(responseTimeKey).await()?.toLongOrNull() ?: 0L
        val failing = producerCommands.get(failingKey).await()?.toBooleanStrictOrNull() ?: false

        return ServiceHealthStatus(failing, responseTime)
    }

    fun addPaymentRequestToQueue(paymentRequest: PaymentRequest) {
        try {
            // log.info("Adding payment request to queue: $paymentRequest")
            producerCommands.lpush(QUEUE_KEY, Json.encodeToString(paymentRequest))
            // log.info("Payment request ${paymentRequest.correlationId} added to queue successfully")
        } catch (e: Exception) {
            log.error("Error adding payment request to queue: ${e.message}", e)
            throw e
        }

    }

    // Consigo dividir isso em diferentes workers?
    fun startRequestConsumer() {
        CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    val result = listenerCommands.brpop(0, QUEUE_KEY).await()
                    //log.info("Request received from queue: $result")
                    if (result != null) {
                        val request = Json.decodeFromString<PaymentRequest>(result.value)
                        PaymentRouterService.processPayment(request)
                    }
                } catch (e: Exception) {
                    log.error("Error processing payment request: ${e.message}", e)
                }
            }
        }
    }

    fun updateProcessorHealthStatus(status: ServiceHealthStatus, service: PaymentService.PaymentProcessorService){
        if(!initialized)
            return

        val responseTimeKey = when (service) {
            PaymentService.PaymentProcessorService.DEFAULT -> DEFAULT_PROCESSOR_RESPONSE_TIME_KEY
            PaymentService.PaymentProcessorService.FALLBACK -> FALLBACK_PROCESSOR_RESPONSE_TIME_KEY
        }
        val failingKey = when (service) {
            PaymentService.PaymentProcessorService.DEFAULT -> DEFAULT_PROCESSOR_FAILING_KEY
            PaymentService.PaymentProcessorService.FALLBACK -> FALLBACK_PROCESSOR_FAILING_KEY
        }
        producerCommands.set(responseTimeKey, status.minResponseTime.toString())
        producerCommands.set(failingKey, status.failing.toString())
    }

    suspend fun resetPayments(){
        try {
            log.info("Resetting processed payments")
            producerCommands.del(PROCESSED_PAYMENTS_SET_KEY).await()
            producerCommands.del(QUEUE_KEY).await()
            log.info("Processed payments reset successfully")
        } catch (e: Exception) {
            log.error("Error resetting processed payments: ${e.message}", e)
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