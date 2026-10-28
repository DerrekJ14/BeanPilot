package com.beanpilot.data.remote

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GaggiMateShotLogParserTest {
    @Test
    fun `parses current shot log sample scales and duration`() {
        val details = GaggiMateShotLogParser.parse(createV8Log(sampleCount = 1))

        assertEquals(1_250L, details.durationMs)
        assertEquals(1, details.samples.size)
        assertEquals(250L, details.samples[0].timeMs)
        assertEquals(92.3, details.samples[0].temperatureC!!, 0.001)
        assertEquals(7.5, details.samples[0].pressureBar!!, 0.001)
        assertEquals(1.23, details.samples[0].flowMlPerSec!!, 0.001)
        assertEquals(12.5, details.samples[0].measuredWeightGr!!, 0.001)
        assertEquals(0.0, details.samples[0].estimatedWeightGr!!, 0.001)
        assertFalse(details.incomplete)
    }

    @Test
    fun `keeps measured and estimated weight as separate series`() {
        val bytes = createV8Log(sampleCount = 1)
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putShort(512 + 4 + 8 * 2, 1_378)
        buffer.putShort(512 + 4 + 9 * 2, 394)

        val sample = GaggiMateShotLogParser.parse(bytes).samples.single()

        assertEquals(137.8, sample.measuredWeightGr!!, 0.001)
        assertEquals(39.4, sample.estimatedWeightGr!!, 0.001)
    }

    @Test
    fun `uses available samples and marks truncated log incomplete`() {
        val details = GaggiMateShotLogParser.parse(createV8Log(sampleCount = 2))

        assertEquals(1, details.samples.size)
        assertEquals(250L, details.durationMs)
        assertTrue(details.incomplete)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid shot log signature`() {
        GaggiMateShotLogParser.parse(ByteArray(512))
    }

    private fun createV8Log(sampleCount: Int): ByteArray {
        val headerSize = 512
        val sampleSize = 30
        val buffer = ByteBuffer.allocate(headerSize + sampleSize).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0, 0x544F4853)
        buffer.put(4, 8)
        buffer.put(5, sampleSize.toByte())
        buffer.putShort(6, headerSize.toShort())
        buffer.putShort(8, 250)
        buffer.putInt(12, 0x3FFF)
        buffer.putInt(16, sampleCount)
        buffer.putInt(20, 1_250)

        var offset = headerSize
        buffer.putInt(offset, 250)
        offset += 4
        repeat(13) { bitIndex ->
            val value = when (bitIndex + 1) {
                2 -> 923
                4 -> 75
                5 -> 123
                9 -> 125
                else -> 0
            }
            buffer.putShort(offset, value.toShort())
            offset += 2
        }
        return buffer.array()
    }
}
