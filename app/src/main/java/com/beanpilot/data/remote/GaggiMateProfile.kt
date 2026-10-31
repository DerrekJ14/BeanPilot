package com.beanpilot.data.remote

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray

@Serializable
data class GaggiMateProfile(
    val id: String? = null,
    val label: String,
    val type: String,
    val description: String,
    val temperature: Double? = null,
    val dose: Double? = null,
    val favorite: Boolean? = null,
    val selected: Boolean? = null,
    val utility: Boolean? = null,
    val phases: List<GaggiMateProfilePhase>,
)

@Serializable
data class GaggiMateProfilePhase(
    val name: String,
    val phase: String,
    val valve: Int,
    val duration: Double,
    val temperature: Double? = null,
    val transition: JsonElement? = null,
    val pump: JsonElement,
    val targets: List<JsonElement>? = null,
)

object GaggiMateProfileCodec {
    fun decode(json: Json, source: String): GaggiMateProfile {
        val element = json.parseToJsonElement(source)
        val profile = json.decodeFromJsonElement<GaggiMateProfile>(element)
        validate(profile)
        return profile
    }

    fun decodeList(json: Json, element: JsonElement): List<GaggiMateProfile> {
        val profiles = element.jsonArray.map { json.decodeFromJsonElement<GaggiMateProfile>(it) }
        profiles.forEach(::validate)
        return profiles
    }

    fun validate(profile: GaggiMateProfile) {
        require(profile.label.isNotBlank()) { "Profile name cannot be empty." }
        require(profile.id == null || profile.id.matches(Regex("^[A-Za-z0-9_-]+$"))) {
            "Profile ID contains unsupported characters."
        }
        require(profile.type in PROFILE_TYPES) { "Profile type must be standard or pro." }
        require(profile.temperature == null || profile.temperature in 0.0..150.0) {
            "Profile temperature must be between 0 and 150 °C."
        }
        require(profile.dose == null || profile.dose in 1.0..100.0) {
            "Profile dose must be between 1 and 100 g."
        }
        require(profile.phases.isNotEmpty()) { "A profile must contain at least one phase." }
        profile.phases.forEachIndexed { index, phase ->
            require(phase.name.isNotBlank()) { "Phase ${index + 1} needs a name." }
            require(phase.phase in PHASE_TYPES) { "Phase ${index + 1} must be preinfusion or brew." }
            require(phase.valve in 0..1) { "Phase ${index + 1} valve must be closed or open." }
            require(phase.duration in 0.5..300.0) {
                "Phase ${index + 1} duration must be between 0.5 and 300 seconds."
            }
            require(phase.temperature == null || phase.temperature in 0.0..160.0) {
                "Phase ${index + 1} temperature must be between 0 and 160 °C."
            }
            validatePump(phase.pump, index)
            phase.targets.orEmpty().forEachIndexed { targetIndex, target ->
                val targetObject = target as? JsonObject
                    ?: throw IllegalArgumentException("Phase ${index + 1} has an invalid stop target.")
                validateTarget(targetObject, index, targetIndex)
            }
            phase.transition?.let {
                val transition = it as? JsonObject
                    ?: throw IllegalArgumentException("Phase ${index + 1} has an invalid transition.")
                validateTransition(transition, index)
            }
        }
    }

    private fun validatePump(pump: JsonElement, phaseIndex: Int) {
        val phase = phaseIndex + 1
        val dutyCycle = (pump as? JsonPrimitive)?.intOrNull
        if (dutyCycle != null) {
            require(dutyCycle in 0..100) { "Phase $phase pump duty must be between 0 and 100%." }
            return
        }
        val advanced = pump as? JsonObject
            ?: throw IllegalArgumentException("Phase $phase pump must be a percentage or control object.")
        val target = (advanced["target"] as? JsonPrimitive)?.contentOrNull
        require(target in PUMP_TARGETS) { "Phase $phase needs a valid pressure or flow pump target." }
        val pressure = (advanced["pressure"] as? JsonPrimitive)?.doubleOrNull
            ?: throw IllegalArgumentException("Phase $phase needs a pressure value.")
        val flow = (advanced["flow"] as? JsonPrimitive)?.doubleOrNull
            ?: throw IllegalArgumentException("Phase $phase needs a flow value.")
        require(pressure == -1.0 || pressure in 0.0..12.0) {
            "Phase $phase pressure must be -1 or between 0 and 12 bar."
        }
        require(flow == -1.0 || flow in 0.0..15.0) {
            "Phase $phase flow must be -1 or between 0 and 15 g/s."
        }
    }

    private fun validateTarget(target: JsonObject, phaseIndex: Int, targetIndex: Int) {
        val label = "Phase ${phaseIndex + 1} target ${targetIndex + 1}"
        val type = (target["type"] as? JsonPrimitive)?.contentOrNull
        val operator = (target["operator"] as? JsonPrimitive)?.contentOrNull ?: "gte"
        val value = (target["value"] as? JsonPrimitive)?.doubleOrNull
        require(type in TARGET_TYPES) { "$label has an unsupported target type." }
        require(operator in OPERATORS) { "$label operator must be gte or lte." }
        require(value != null && value >= 0.0) { "$label value must be zero or greater." }
    }

    private fun validateTransition(transition: JsonObject, phaseIndex: Int) {
        val label = "Phase ${phaseIndex + 1} transition"
        val type = (transition["type"] as? JsonPrimitive)?.contentOrNull
        val duration = (transition["duration"] as? JsonPrimitive)?.doubleOrNull
        require(type in TRANSITION_TYPES) { "$label has an unsupported easing type." }
        require(duration != null && duration >= 0.0) { "$label duration must be zero or greater." }
        require((transition["adaptive"] as? JsonPrimitive)?.contentOrNull in setOf("true", "false")) {
            "$label must specify whether the ramp is adaptive."
        }
    }

    private val PROFILE_TYPES = setOf("standard", "pro")
    private val PHASE_TYPES = setOf("preinfusion", "brew")
    private val PUMP_TARGETS = setOf("pressure", "flow")
    private val TARGET_TYPES = setOf("volumetric", "ratio", "pressure", "flow", "pumped")
    private val OPERATORS = setOf("gte", "lte")
    private val TRANSITION_TYPES = setOf("instant", "linear", "ease-in", "ease-out", "ease-in-out")
}
