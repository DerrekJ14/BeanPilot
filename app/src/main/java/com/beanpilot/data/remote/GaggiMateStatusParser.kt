package com.beanpilot.data.remote

import com.beanpilot.domain.model.TelemetrySnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class GaggiMateStatusParser(
    private val json: Json,
) {
    fun apply(message: String, previous: TelemetrySnapshot): TelemetrySnapshot? {
        val payload = json.parseToJsonElement(message).jsonObject
        if (payload["tp"]?.jsonPrimitive?.contentOrNull != "evt:status") return null

        return previous.copy(
            machineState = payload.systemStateOrPrevious(previous.machineState),
            temperatureC = payload.numberOrPrevious("ct", previous.temperatureC),
            pressureBar = payload.numberOrPrevious("pr", previous.pressureBar),
            flowMlPerMin = payload.numberOrPrevious("fl", previous.flowMlPerMin),
            activeProfile = payload.stringOrPrevious("p", previous.activeProfile),
            timestamp = System.currentTimeMillis(),
        )
    }

    private fun JsonObject.systemStateOrPrevious(previous: String?): String? {
        if (!containsKey("sys")) return previous
        val system = this["sys"] ?: return null
        if (system is JsonNull) return null
        if (system !is JsonObject) {
            throw IllegalArgumentException("GaggiMate status field 'sys' must be an object or null.")
        }
        return system.stringOrPrevious("s", previous)
    }

    private fun JsonObject.stringOrPrevious(
        key: String,
        previous: String?,
    ): String? {
        if (!containsKey(key)) return previous
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        if (value !is JsonPrimitive) {
            throw IllegalArgumentException("GaggiMate status field '$key' must be a string or null.")
        }
        return value.contentOrNull
    }

    private fun JsonObject.numberOrPrevious(
        key: String,
        previous: Double?,
    ): Double? {
        if (!containsKey(key)) return previous
        val value = this[key] ?: return null
        if (value is JsonNull) return null
        if (value !is JsonPrimitive) {
            throw IllegalArgumentException("GaggiMate status field '$key' must be a number or null.")
        }
        return value.jsonPrimitive.doubleOrNull
            ?: throw IllegalArgumentException("GaggiMate status field '$key' must be a number or null.")
    }
}
