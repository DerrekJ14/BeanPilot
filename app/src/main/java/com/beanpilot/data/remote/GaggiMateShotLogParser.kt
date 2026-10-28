package com.beanpilot.data.remote

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GaggiMateShotDetails(
    val durationMs: Long,
    val samples: List<GaggiMateShotSample>,
    val incomplete: Boolean,
)

data class GaggiMateShotSample(
    val timeMs: Long,
    val temperatureC: Double?,
    val pressureBar: Double?,
    val flowMlPerSec: Double?,
    val measuredWeightGr: Double?,
    val estimatedWeightGr: Double?,
)

object GaggiMateShotLogParser {
    private const val MAGIC = 0x544F4853
    private const val HEADER_SIZE_V4 = 128
    private const val HEADER_SIZE_V5 = 512
    private const val TIME_BIT = 0
    private const val TEMPERATURE_BIT = 2
    private const val PRESSURE_BIT = 4
    private const val FLOW_BIT = 5
    private const val WEIGHT_BIT = 9
    private const val ESTIMATED_WEIGHT_BIT = 10

    fun parse(bytes: ByteArray): GaggiMateShotDetails {
        require(bytes.size >= 16) { "GaggiMate shot log is too small for a header." }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        require(buffer.getInt(0) == MAGIC) { "GaggiMate shot log has an invalid signature." }

        val version = buffer.get(4).toInt() and 0xFF
        require(version in 1..8) { "Unsupported GaggiMate shot log version: $version." }
        val sampleSize = buffer.get(5).toInt() and 0xFF
        val headerSize = buffer.getShort(6).toInt() and 0xFFFF
        val expectedHeaderSize = if (version <= 4) HEADER_SIZE_V4 else HEADER_SIZE_V5
        require(headerSize == expectedHeaderSize && bytes.size >= headerSize) {
            "GaggiMate shot log has an invalid header size."
        }

        val sampleInterval = buffer.getShort(8).toInt() and 0xFFFF
        val fieldsMask = buffer.getInt(12)
        require(fieldsMask != 0) { "GaggiMate shot log has an empty field mask." }
        val fieldCount = Integer.bitCount(fieldsMask)
        val legacyScaleV6 = version == 6 && sampleSize == 26 && fieldsMask == 0x1FFF
        val elapsedTimestamp = version >= 6 && !legacyScaleV6
        val expectedSampleSize = fieldCount * 2 + if (elapsedTimestamp) 2 else 0
        require(sampleSize == expectedSampleSize) {
            "GaggiMate shot log sample layout is unsupported."
        }

        val dataSize = bytes.size - headerSize
        val completeSampleCount = dataSize / sampleSize
        val trailingBytes = dataSize % sampleSize
        val expectedSampleCount = buffer.getInt(16).toLong() and 0xFFFFFFFFL
        val sampleCount = if (expectedSampleCount == 0L) {
            completeSampleCount
        } else {
            minOf(expectedSampleCount, completeSampleCount.toLong()).toInt()
        }
        val declaredCountTruncated = expectedSampleCount > completeSampleCount
        val samples = ArrayList<GaggiMateShotSample>(sampleCount)

        repeat(sampleCount) { index ->
            val base = headerSize + index * sampleSize
            var offset = base
            var timeMs: Long? = null
            var temperature: Double? = null
            var pressure: Double? = null
            var flow: Double? = null
            var measuredWeight: Double? = null
            var estimatedWeight: Double? = null

            for (bit in 0..31) {
                if (fieldsMask and (1 shl bit) == 0) continue
                val isElapsedTime = bit == TIME_BIT && elapsedTimestamp
                val raw: Double = when {
                    isElapsedTime ->
                        (buffer.getInt(offset).toLong() and 0xFFFFFFFFL).toDouble()
                    bit in SIGNED_FLOW_BITS -> buffer.getShort(offset).toInt().toDouble()
                    else -> (buffer.getShort(offset).toInt() and 0xFFFF).toDouble()
                }
                when (bit) {
                    TIME_BIT -> timeMs = if (elapsedTimestamp) raw.toLong() else {
                        (raw * sampleInterval).toLong()
                    }
                    TEMPERATURE_BIT -> temperature = raw / 10.0
                    PRESSURE_BIT -> pressure = raw / 10.0
                    FLOW_BIT -> flow = raw / 100.0
                    WEIGHT_BIT -> measuredWeight = raw / 10.0
                    ESTIMATED_WEIGHT_BIT -> estimatedWeight = raw / 10.0
                }
                offset += if (isElapsedTime) 4 else 2
            }

            samples += GaggiMateShotSample(
                timeMs = timeMs ?: index.toLong() * sampleInterval,
                temperatureC = temperature,
                pressureBar = pressure,
                flowMlPerSec = flow,
                measuredWeightGr = measuredWeight,
                estimatedWeightGr = estimatedWeight,
            )
        }

        val durationHeaderMs = buffer.getInt(20).toLong() and 0xFFFFFFFFL
        val incomplete = expectedSampleCount == 0L ||
            trailingBytes != 0 ||
            declaredCountTruncated
        val durationMs = if (!incomplete && durationHeaderMs > 0) {
            durationHeaderMs
        } else {
            samples.lastOrNull()?.timeMs ?: 0L
        }

        return GaggiMateShotDetails(
            durationMs = durationMs,
            samples = samples,
            incomplete = incomplete,
        )
    }

    private val SIGNED_FLOW_BITS = setOf(5, 6, 7, 8)
}
