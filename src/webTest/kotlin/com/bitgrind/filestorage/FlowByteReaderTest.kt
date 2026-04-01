@file:OptIn(ExperimentalCoroutinesApi::class, ExperimentalWasmJsInterop::class, ExperimentalUnsignedTypes::class)

package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.OpfsByteReader
import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import js.typedarrays.toUint8Array
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlowByteReaderTest {

    /** Converts a [ByteArray] to a [Uint8Array] chunk suitable for passing to [com.bitgrind.filestorage.impl.OpfsByteReader]. */
    private fun chunk(bytes: ByteArray): Uint8Array<ArrayBuffer> = bytes.toUint8Array()


    // --- readShort ---

    @Test
    fun testReadShortWithinChunk() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x01, 0x02))), this)
        assertEquals(0x0102.toShort(), reader.readShort())
    }

    @Test
    fun testReadShortAcrossChunkBoundary() = runTest {
        // High byte in first chunk, low byte in second
        val reader = OpfsByteReader(
            flowOf(chunk(byteArrayOf(0x12)), chunk(byteArrayOf(0x34))),
            this
        )
        assertEquals(0x1234.toShort(), reader.readShort())
    }

    @Test
    fun testReadShortNegativeValue() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0xFF.toByte(), 0xFF.toByte()))), this)
        assertEquals((-1).toShort(), reader.readShort())
    }

    @Test
    fun testReadShortMaxValue() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x7F, 0xFF.toByte()))), this)
        assertEquals(Short.MAX_VALUE, reader.readShort())
    }

    // --- readInt ---

    @Test
    fun testReadIntWithinChunk() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x01, 0x02, 0x03, 0x04))), this)
        assertEquals(0x01020304, reader.readInt())
    }

    @Test
    fun testReadIntSplitAcrossTwoChunks() = runTest {
        val reader = OpfsByteReader(
            flowOf(chunk(byteArrayOf(0x00, 0x00)), chunk(byteArrayOf(0x00, 0x01))),
            this
        )
        assertEquals(1, reader.readInt())
    }

    @Test
    fun testReadIntSplitOneByteAtATime() = runTest {
        val reader = OpfsByteReader(
            flowOf(
                chunk(byteArrayOf(0x01)),
                chunk(byteArrayOf(0x02)),
                chunk(byteArrayOf(0x03)),
                chunk(byteArrayOf(0x04)),
            ),
            this
        )
        assertEquals(0x01020304, reader.readInt())
    }

    @Test
    fun testReadIntNegativeValue() = runTest {
        val reader = OpfsByteReader(
            flowOf(chunk(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte()))),
            this
        )
        assertEquals(-1, reader.readInt())
    }

    // --- readString ---

    @Test
    fun testReadStringWithinChunk() = runTest {
        val text = "hello"
        val bytes = text.encodeToByteArray()
        val frame = chunk(byteArrayOf(0x00, bytes.size.toByte()) + bytes)
        val reader = OpfsByteReader(flowOf(frame), this)
        assertEquals(text, reader.readString())
    }

    @Test
    fun testReadStringAcrossChunkBoundary() = runTest {
        val text = "world"
        val bytes = text.encodeToByteArray()
        val reader = OpfsByteReader(
            flowOf(
                chunk(byteArrayOf(0x00, bytes.size.toByte()) + bytes.copyOf(2)),
                chunk(bytes.copyOfRange(2, bytes.size)),
            ),
            this
        )
        assertEquals(text, reader.readString())
    }

    @Test
    fun testReadStringMultibyteUtf8() = runTest {
        val text = "caf\u00e9" // "café" — 'é' is 2 bytes in UTF-8
        val bytes = text.encodeToByteArray()
        val frame = chunk(byteArrayOf((bytes.size shr 8).toByte(), bytes.size.toByte()) + bytes)
        val reader = OpfsByteReader(flowOf(frame), this)
        assertEquals(text, reader.readString())
    }

    @Test
    fun testReadEmptyString() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x00, 0x00))), this)
        assertEquals("", reader.readString())
    }

    // --- readByteArray ---

    @Test
    fun testReadByteArrayWithinChunk() = runTest {
        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val frame = chunk(byteArrayOf(0x00, payload.size.toByte()) + payload)
        val reader = OpfsByteReader(flowOf(frame), this)
        assertContentEquals(payload, reader.readByteArray())
    }

    @Test
    fun testReadByteArrayAcrossChunkBoundary() = runTest {
        val payload = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        val reader = OpfsByteReader(
            flowOf(
                chunk(byteArrayOf(0x00)),
                chunk(byteArrayOf(payload.size.toByte()) + payload.copyOf(3)),
                chunk(payload.copyOfRange(3, payload.size)),
            ),
            this
        )
        assertContentEquals(payload, reader.readByteArray())
    }

    @Test
    fun testReadEmptyByteArray() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x00, 0x00))), this)
        assertContentEquals(byteArrayOf(), reader.readByteArray())
    }

    // --- multi-value sequences ---

    @Test
    fun testReadMultipleValuesFromSingleChunk() = runTest {
        val text = "hi"
        val textBytes = text.encodeToByteArray()
        val payload = chunk(
            byteArrayOf(0x01, 0x02) +                              // short  = 0x0102
                    byteArrayOf(0x00, 0x00, 0x00, 0x2A) +                  // int    = 42
                    byteArrayOf(0x00, textBytes.size.toByte()) + textBytes
        )
        val reader = OpfsByteReader(flowOf(payload), this)
        assertEquals(0x0102.toShort(), reader.readShort())
        assertEquals(42, reader.readInt())
        assertEquals(text, reader.readString())
    }

    @Test
    fun testReadMultipleValuesAcrossChunks() = runTest {
        // Intentionally tiny 1-byte chunks to force every possible boundary crossing
        val short: Short = 300
        val int = 0xCAFEBABE.toInt()
        val text = "ok"
        val textBytes = text.encodeToByteArray()
        val payload =
            byteArrayOf((short.toInt() shr 8).toByte(), short.toByte()) +
            byteArrayOf((int shr 24).toByte(), (int shr 16).toByte(), (int shr 8).toByte(), int.toByte()) +
            byteArrayOf(0x00, textBytes.size.toByte()) + textBytes

        val chunks = payload.map { chunk(byteArrayOf(it)) }
        val reader = OpfsByteReader(flow { chunks.forEach { emit(it) } }, this)
        assertEquals(short, reader.readShort())
        assertEquals(int, reader.readInt())
        assertEquals(text, reader.readString())
    }

    // --- error handling ---

    @Test
    fun testReadShortPastEndOfStream() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x01))), this)
        assertFailsWith<IOException> { reader.readShort() }
    }

    @Test
    fun testReadIntPastEndOfStream() = runTest {
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x01, 0x02))), this)
        assertFailsWith<IOException> { reader.readInt() }
    }

    @Test
    fun testReadStringPastEndOfStream() = runTest {
        // Length header says 10 bytes but stream only has 3
        val reader = OpfsByteReader(flowOf(chunk(byteArrayOf(0x00, 0x0A, 0x01, 0x02, 0x03))), this)
        assertFailsWith<IOException> { reader.readString() }
    }
}
