package com.beanpilot.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.http.isSuccess
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

@Singleton
class GaggiMateHistoryClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val requestIds = AtomicLong()

    suspend fun fetchRecentShots(address: String): List<GaggiMateShot> {
        val endpoint = GaggiMateEndpoint.parse(address)
        val response: HttpResponse = httpClient.get(endpoint.historyUrl) {
            parameter("limit", HISTORY_LIMIT)
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                "GaggiMate history request failed (HTTP ${response.status.value}).",
            )
        }
        return GaggiMateShotIndexParser.parse(response.body())
    }

    suspend fun fetchShotDetails(address: String, sourceId: String): GaggiMateShotDetails {
        val endpoint = GaggiMateEndpoint.parse(address)
        val response: HttpResponse = httpClient.get(endpoint.shotLogUrl(sourceId))
        if (!response.status.isSuccess()) {
            throw IllegalStateException(
                "GaggiMate shot request failed (HTTP ${response.status.value}).",
            )
        }
        return GaggiMateShotLogParser.parse(response.body())
    }

    suspend fun fetchShotNotes(address: String, sourceId: String): GaggiMateShotNotes? {
        val endpoint = GaggiMateEndpoint.parse(address)
        val requestId = requestIds.incrementAndGet().toString()
        val response = sendNotesRequest(
            endpoint = endpoint,
            requestId = requestId,
            request = buildJsonObject {
                put("tp", "req:history:notes:get")
                put("rid", requestId)
                put("id", sourceId)
            },
            expectedType = "res:history:notes:get",
        )
        val notes = response["notes"]
            ?: throw IllegalStateException("GaggiMate returned a notes response without notes.")
        return if (notes.toString() == "null") {
            null
        } else {
            json.decodeFromJsonElement<GaggiMateShotNotes>(notes)
        }
    }

    suspend fun saveShotNotes(
        address: String,
        sourceId: String,
        notes: GaggiMateShotNotes,
    ) {
        val endpoint = GaggiMateEndpoint.parse(address)
        val requestId = requestIds.incrementAndGet().toString()
        val response = sendNotesRequest(
            endpoint = endpoint,
            requestId = requestId,
            request = buildJsonObject {
                put("tp", "req:history:notes:save")
                put("rid", requestId)
                put("id", sourceId)
                put("notes", json.encodeToJsonElement(GaggiMateShotNotes.serializer(), notes))
            },
            expectedType = "res:history:notes:save",
        )
        val message = response["msg"]?.toString()?.trim('"')
        check(message == "Ok") {
            "GaggiMate did not confirm saving shot notes${message?.let { ": $it" } ?: "."}"
        }
    }

    private suspend fun sendNotesRequest(
        endpoint: GaggiMateEndpoint,
        requestId: String,
        request: JsonObject,
        expectedType: String,
    ): JsonObject {
        val session = httpClient.webSocketSession {
            url(endpoint.webSocketUrl)
        }
        try {
            session.send(Frame.Text(json.encodeToString(JsonObject.serializer(), request)))
            return withTimeout(NOTES_REQUEST_TIMEOUT_MS) {
                for (frame in session.incoming) {
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val response = try {
                        json.parseToJsonElement(text).jsonObject
                    } catch (_: IllegalArgumentException) {
                        continue
                    }
                    if (
                        response["rid"]?.toString()?.trim('"') == requestId &&
                        response["tp"]?.toString()?.trim('"') == expectedType
                    ) {
                        return@withTimeout response
                    }
                }
                throw IllegalStateException("GaggiMate closed the connection before replying.")
            }
        } finally {
            session.close()
        }
    }

    private companion object {
        const val HISTORY_LIMIT = 50
        const val NOTES_REQUEST_TIMEOUT_MS = 10_000L
    }
}
