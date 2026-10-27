package com.beanpilot.data.remote

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GaggiMateShot(
    val sourceId: String,
    val timestampSeconds: Long,
    val durationMs: Long,
    val doseOutGr: Double?,
    val rating: Int?,
    val profileName: String,
    val incomplete: Boolean,
)

object GaggiMateShotIndexParser {
    private const val HEADER_SIZE = 32
    private const val ENTRY_SIZE = 128
    private const val MAGIC = 0x58444953
    private const val DELETED_FLAG = 0x02
    private const val COMPLETED_FLAG = 0x01

    fun parse(bytes: ByteArray): List<GaggiMateShot> {
        require(bytes.size >= HEADER_SIZE) { "GaggiMate shot index is too small." }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.getInt(0) == MAGIC) { "GaggiMate shot index has an invalid signature." }

        val entrySize = buffer.getShort(6).toInt() and 0xFFFF
        require(entrySize == ENTRY_SIZE) {
            "Unsupported GaggiMate shot index entry size: $entrySize."
        }

        val entryCount = buffer.getInt(8).toLong() and 0xFFFFFFFFL
        val expectedSize = HEADER_SIZE.toLong() + entryCount * ENTRY_SIZE
        require(expectedSize <= bytes.size) {
            "GaggiMate shot index is truncated (expected $expectedSize bytes, received ${bytes.size})."
        }
        require(entryCount <= MAX_ENTRY_COUNT) { "GaggiMate shot index contains too many entries." }

        return (0 until entryCount.toInt()).mapNotNull { index ->
            val base = HEADER_SIZE + index * ENTRY_SIZE
            val flags = buffer.get(base + 15).toInt() and 0xFF
            if (flags and DELETED_FLAG != 0) return@mapNotNull null

            val timestamp = buffer.getInt(base + 4).toLong() and 0xFFFFFFFFL
            val duration = buffer.getInt(base + 8).toLong() and 0xFFFFFFFFL
            val volume = buffer.getShort(base + 12).toInt() and 0xFFFF
            val rating = buffer.get(base + 14).toInt() and 0xFF

            GaggiMateShot(
                sourceId = (buffer.getInt(base).toLong() and 0xFFFFFFFFL).toString(),
                timestampSeconds = timestamp,
                durationMs = duration,
                doseOutGr = volume.takeIf { it > 0 }?.div(10.0),
                rating = rating.takeIf { it in 1..5 },
                profileName = decodeCString(bytes, base + 48, 48),
                incomplete = flags and COMPLETED_FLAG == 0,
            )
        }.sortedByDescending { it.timestampSeconds }
    }

    private fun decodeCString(bytes: ByteArray, offset: Int, length: Int): String {
        val end = (offset until offset + length).firstOrNull { bytes[it].toInt() == 0 }
            ?: offset + length
        return bytes.copyOfRange(offset, end).toString(Charsets.UTF_8)
    }

    private const val MAX_ENTRY_COUNT = 100_000L
}
