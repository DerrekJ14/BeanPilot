package com.beanpilot.data.remote

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GaggiMateShotIndexParserTest {
    @Test
    fun `parses fields and skips deleted shots from the binary index`() {
        val index = createIndex(
            shot(id = 1, timestamp = 1_700_000_000, durationMs = 29_500, volume = 364, rating = 5),
            shot(id = 2, timestamp = 1_700_000_100, durationMs = 31_000, volume = 0, rating = 0, flags = 0x03),
            shot(id = 3, timestamp = 1_700_000_200, durationMs = 8_000, volume = 280, rating = 3, flags = 0x00),
        )

        val result = GaggiMateShotIndexParser.parse(index)

        assertEquals(2, result.size)
        assertEquals("3", result[0].sourceId)
        assertEquals(1_700_000_200L, result[0].timestampSeconds)
        assertEquals(8_000L, result[0].durationMs)
        assertEquals(28.0, result[0].doseOutGr)
        assertEquals(3, result[0].rating)
        assertTrue(result[0].incomplete)
        assertEquals("Espresso", result[0].profileName)
        assertEquals("1", result[1].sourceId)
        assertFalse(result[1].incomplete)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid index magic`() {
        GaggiMateShotIndexParser.parse(ByteArray(32))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects truncated index`() {
        val index = createIndex(shot(id = 1))
        GaggiMateShotIndexParser.parse(index.copyOf(50))
    }

    private fun createIndex(vararg entries: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(32 + entries.size * 128).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0x58444953)
        buffer.putShort(4, 1)
        buffer.putShort(6, 128)
        buffer.putInt(8, entries.size)
        entries.forEachIndexed { index, entry ->
            val base = 32 + index * 128
            for (offset in entry.indices) buffer.put(base + offset, entry[offset])
        }
        return buffer.array()
    }

    private fun shot(
        id: Int,
        timestamp: Int = 1_700_000_000,
        durationMs: Int = 25_000,
        volume: Int = 360,
        rating: Int = 4,
        flags: Int = 0x01,
    ): ByteArray {
        val buffer = ByteBuffer.allocate(128).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(0, id)
        buffer.putInt(4, timestamp)
        buffer.putInt(8, durationMs)
        buffer.putShort(12, volume.toShort())
        buffer.put(14, rating.toByte())
        buffer.put(15, flags.toByte())
        val profile = "Espresso".toByteArray(Charsets.UTF_8)
        profile.forEachIndexed { index, byte -> buffer.put(48 + index, byte) }
        return buffer.array()
    }
}
