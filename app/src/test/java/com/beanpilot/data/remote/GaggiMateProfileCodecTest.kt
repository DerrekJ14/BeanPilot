package com.beanpilot.data.remote

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class GaggiMateProfileCodecTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a multi-phase pressure profile with targets and transition`() {
        val profile = GaggiMateProfileCodec.decode(
            json,
            """
                {
                  "label":"Test profile",
                  "type":"pro",
                  "description":"Two phase pressure profile",
                  "temperature":93,
                  "phases":[
                    {
                      "name":"Preinfusion",
                      "phase":"preinfusion",
                      "valve":1,
                      "duration":8,
                      "temperature":0,
                      "transition":{"type":"linear","duration":3,"adaptive":true},
                      "pump":{"target":"pressure","pressure":3,"flow":0},
                      "targets":[{"type":"pressure","operator":"gte","value":3}]
                    },
                    {
                      "name":"Brew",
                      "phase":"brew",
                      "valve":1,
                      "duration":25,
                      "pump":{"target":"pressure","pressure":9,"flow":0},
                      "targets":[{"type":"volumetric","operator":"gte","value":36}]
                    }
                  ]
                }
            """.trimIndent(),
        )

        assertEquals("Test profile", profile.label)
        assertEquals(2, profile.phases.size)
        assertEquals("preinfusion", profile.phases.first().phase)
        assertEquals(9.0, profile.phases.last().pump.jsonObject["pressure"]!!.jsonPrimitive.double, 0.0)
    }

    @Test
    fun `rejects invalid phase duration and unsupported targets`() {
        val invalidDuration = sampleProfile().copy(
            phases = listOf(sampleProfile().phases.first().copy(duration = 301.0)),
        )
        val invalidTarget = sampleProfile().copy(
            phases = listOf(
                sampleProfile().phases.first().copy(
                    targets = listOf(
                        json.parseToJsonElement(
                            """{"type":"weight","operator":"gte","value":36}""",
                        ),
                    ),
                ),
            ),
        )

        assertThrows(IllegalArgumentException::class.java) {
            GaggiMateProfileCodec.validate(invalidDuration)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GaggiMateProfileCodec.validate(invalidTarget)
        }
    }

    private fun sampleProfile() = GaggiMateProfile(
        label = "Test",
        type = "pro",
        description = "",
        phases = listOf(
            GaggiMateProfilePhase(
                name = "Brew",
                phase = "brew",
                valve = 1,
                duration = 25.0,
                pump = json.parseToJsonElement(
                    """{"target":"pressure","pressure":9,"flow":0}""",
                ),
            ),
        ),
    )
}
