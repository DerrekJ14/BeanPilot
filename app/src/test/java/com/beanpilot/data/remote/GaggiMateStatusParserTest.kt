package com.beanpilot.data.remote

import com.beanpilot.domain.model.TelemetrySnapshot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GaggiMateStatusParserTest {
    private val parser = GaggiMateStatusParser(Json)

    @Test
    fun `parses compact GaggiMate status fields`() {
        val result = parser.apply(
            """{"tp":"evt:status","ct":93.2,"pr":8.4,"fl":2.1,"p":"Espresso","sys":{"s":"ready"}}""",
            TelemetrySnapshot(),
        )

        assertEquals(93.2, result?.temperatureC)
        assertEquals(8.4, result?.pressureBar)
        assertEquals(2.1, result?.flowMlPerMin)
        assertEquals("Espresso", result?.activeProfile)
        assertEquals("ready", result?.machineState)
    }

    @Test
    fun `partial status only updates fields included in the event`() {
        val previous = TelemetrySnapshot(
            temperatureC = 92.5,
            pressureBar = 7.0,
            flowMlPerMin = 1.5,
            activeProfile = "Classic",
        )

        val result = parser.apply(
            """{"tp":"evt:status","pr":8.0}""",
            previous,
        )

        assertEquals(92.5, result?.temperatureC)
        assertEquals(8.0, result?.pressureBar)
        assertEquals(1.5, result?.flowMlPerMin)
        assertEquals("Classic", result?.activeProfile)
    }

    @Test
    fun `null status field clears the previously known value`() {
        val result = parser.apply(
            """{"tp":"evt:status","ct":null,"p":null,"sys":null}""",
            TelemetrySnapshot(
                temperatureC = 92.5,
                activeProfile = "Classic",
                machineState = "ready",
            ),
        )

        assertNull(result?.temperatureC)
        assertNull(result?.activeProfile)
        assertNull(result?.machineState)
    }

    @Test
    fun `ignores non-status events`() {
        val result = parser.apply(
            """{"tp":"evt:ota-progress","progress":25}""",
            TelemetrySnapshot(),
        )

        assertNull(result)
    }
}
