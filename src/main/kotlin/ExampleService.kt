package app.cesario

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.request
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class ExampleService {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            })
        }
    }

    suspend fun getTodo(): Todo {
        return client.get("https://jsonplaceholder.typicode.com/todos/1").body()
    }

    @Serializable
    data class Todo(
        val userId: Int,
        val id: Int,
        val title: String,
        val completed: Boolean
    )
}