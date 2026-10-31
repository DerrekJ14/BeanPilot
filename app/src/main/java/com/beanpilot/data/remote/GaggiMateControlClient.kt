package com.beanpilot.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.url
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Singleton
class GaggiMateControlClient @Inject constructor(
    private val httpClient: HttpClient,
    private val json: Json,
) {
    private val requestIds = AtomicLong()

    suspend fun listProfiles(address: String): List<GaggiMateProfile> {
        val response = request(address, "req:profiles:list", "res:profiles:list")
        val profiles = response["profiles"]
            ?: throw IllegalStateException("GaggiMate returned a profile list without profiles.")
        return GaggiMateProfileCodec.decodeList(json, profiles)
    }

    suspend fun loadProfile(address: String, id: String): GaggiMateProfile {
        val response = request(address, "req:profiles:load", "res:profiles:load") {
            put("id", id)
        }
        val profile = response["profile"]
            ?: throw IllegalStateException("GaggiMate returned a profile response without a profile.")
        return json.decodeFromJsonElement<GaggiMateProfile>(profile).also(GaggiMateProfileCodec::validate)
    }

    suspend fun saveProfile(address: String, profile: GaggiMateProfile) {
        GaggiMateProfileCodec.validate(profile)
        request(address, "req:profiles:save", "res:profiles:save") {
            put(
                "profile",
                json.encodeToJsonElement(GaggiMateProfile.serializer(), profile),
            )
        }
    }

    suspend fun selectProfile(address: String, id: String) {
        request(address, "req:profiles:select", "res:profiles:select") {
            put("id", id)
        }
    }

    private suspend fun request(
        address: String,
        requestType: String,
        responseType: String,
        addFields: JsonObjectBuilder.() -> Unit = {},
    ): JsonObject {
        val endpoint = GaggiMateEndpoint.parse(address)
        val rid = requestIds.incrementAndGet().toString()
        val message = buildJsonObject {
            put("tp", requestType)
            put("rid", rid)
            addFields()
        }
        val session = httpClient.webSocketSession { url(endpoint.webSocketUrl) }
        try {
            session.send(Frame.Text(json.encodeToString(JsonObject.serializer(), message)))
            val response = withTimeout(REQUEST_TIMEOUT_MS) {
                for (frame in session.incoming) {
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    val candidate = parseObjectOrNull(text) ?: continue
                    if (
                        candidate["rid"]?.jsonPrimitive?.contentOrNull == rid &&
                        candidate["tp"]?.jsonPrimitive?.contentOrNull == responseType
                    ) {
                        val error = candidate["error"]?.jsonPrimitive?.contentOrNull
                        check(error.isNullOrBlank()) {
                            "GaggiMate rejected $requestType: $error"
                        }
                        return@withTimeout candidate
                    }
                }
                throw IllegalStateException("GaggiMate closed the connection before replying.")
            }
            return response
        } finally {
            session.close()
        }
    }

    private fun parseObjectOrNull(text: String): JsonObject? =
        try {
            json.parseToJsonElement(text).jsonObject
        } catch (_: IllegalArgumentException) {
            null
        }

    private companion object {
        const val REQUEST_TIMEOUT_MS = 10_000L
    }
}
