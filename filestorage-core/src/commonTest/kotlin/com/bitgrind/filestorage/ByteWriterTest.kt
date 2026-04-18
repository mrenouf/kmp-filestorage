package com.bitgrind.filestorage

import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith

class ByteWriterTest {

    private fun bufferWriter(): Pair<ByteWriter, () -> ByteArray> {
        val buffer = Buffer()
        val writer = object : ByteWriter {
            override suspend fun writeByte(byte: Byte) { buffer.writeByte(byte) }
            override suspend fun writeShort(short: Short) { buffer.writeShort(short) }
            override suspend fun writeInt(int: Int) { buffer.writeInt(int) }
            override suspend fun writeLong(long: Long) { buffer.writeLong(long) }
            override suspend fun writeString(string: String) { buffer.write(string.encodeToByteArray()) }
            override suspend fun writeByteArray(array: ByteArray) { buffer.write(array) }
            override suspend fun writeByteArray(array: ByteArray, offset: Int, length: Int) { buffer.write(array, offset, offset + length) }
            override suspend fun close() {}
        }
        return writer to { buffer.readByteArray() }
    }

    // --- writeVarint ---

    @Test
    fun testWriteVarintZero() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(0L)
        assertContentEquals(byteArrayOf(0x00), bytes())
    }

    @Test
    fun testWriteVarintOne() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(1L)
        assertContentEquals(byteArrayOf(0x01), bytes())
    }

    @Test
    fun testWriteVarintMaxOneByte() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(127L)
        assertContentEquals(byteArrayOf(0x7F), bytes())
    }

    @Test
    fun testWriteVarintMinTwoBytes() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(128L)
        assertContentEquals(byteArrayOf(0x80.toByte(), 0x01), bytes())
    }

    @Test
    fun testWriteVarintThreeHundred() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(300L)
        assertContentEquals(byteArrayOf(0xAC.toByte(), 0x02), bytes())
    }

    @Test
    fun testWriteVarintMaxTwoBytes() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(16383L)
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0x7F), bytes())
    }

    @Test
    fun testWriteVarintIntMaxValue() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(Int.MAX_VALUE.toLong())
        assertContentEquals(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x07), bytes())
    }

    @Test
    fun testWriteVarintLongMaxValue() = runTest {
        val (writer, bytes) = bufferWriter()
        writer.writeVarint(Long.MAX_VALUE)
        assertContentEquals(byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F
        ), bytes())
    }

    @Test
    fun testWriteVarintNegativeThrows() = runTest {
        val (writer, _) = bufferWriter()
        assertFailsWith<IllegalArgumentException> { writer.writeVarint(-1L) }
    }

    // --- roundtrip ---

    @Test
    fun testVarintRoundtrip() = runTest {
        val values = listOf(0L, 1L, 127L, 128L, 300L, 16383L, 16384L, Int.MAX_VALUE.toLong(), Long.MAX_VALUE)
        for (value in values) {
            val (writer, bytes) = bufferWriter()
            writer.writeVarint(value)
            val reader = com.bitgrind.filestorage.testing.byteReaderOf(bytes())
            kotlin.test.assertEquals(value, reader.readVarint(), "roundtrip failed for $value")
        }
    }
}
