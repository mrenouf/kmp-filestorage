package com.bitgrind.filestorage

import com.bitgrind.filestorage.testing.byteReaderOf
import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ByteReaderTest {

    // 2-byte sequence: é (U+00E9) = 0xC3 0xA9 — split 1+1
    @Test
    fun testTwoByteCharSplitAtBoundary() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xC3.toByte()),        // 'a', lead byte of é
            byteArrayOf(0xA9.toByte(), 0x62),         // trail byte of é, 'b'
        )
        assertEquals("aéb", reader.readString(4))
    }

    // 3-byte sequence: € (U+20AC) = 0xE2 0x82 0xAC — split 2+1
    @Test
    fun testThreeByteCharSplit2_1() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xE2.toByte(), 0x82.toByte()), // 'a', first 2 bytes of €
            byteArrayOf(0xAC.toByte(), 0x62),                 // last byte of €, 'b'
        )
        assertEquals("a€b", reader.readString(5))
    }

    // 3-byte sequence: € (U+20AC) = 0xE2 0x82 0xAC — split 1+2
    @Test
    fun testThreeByteCharSplit1_2() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xE2.toByte()),                  // 'a', lead byte of €
            byteArrayOf(0x82.toByte(), 0xAC.toByte(), 0x62),  // last 2 bytes of €, 'b'
        )
        assertEquals("a€b", reader.readString(5))
    }

    // 4-byte sequence: 😀 (U+1F600) = 0xF0 0x9F 0x98 0x80 — split 3+1
    @Test
    fun testFourByteCharSplit3_1() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xF0.toByte(), 0x9F.toByte(), 0x98.toByte()), // 'a', first 3 bytes of 😀
            byteArrayOf(0x80.toByte(), 0x62),                                // last byte of 😀, 'b'
        )
        assertEquals("a😀b", reader.readString(6))
    }

    // 4-byte sequence: 😀 (U+1F600) — split across 3 chunks: 2 + 2 + 2
    @Test
    fun testFourByteCharSplitAcrossThreeChunks() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xF0.toByte()),                 // 'a', first byte of 😀
            byteArrayOf(0x9F.toByte(), 0x98.toByte()),        // middle 2 bytes of 😀
            byteArrayOf(0x80.toByte(), 0x62),                 // last byte of 😀, 'b'
        )
        assertEquals("a😀b", reader.readString(6))
    } // --- readVarint ---

    @Test
    fun testReadVarintZero() = runTest {
        val reader = byteReaderOf(byteArrayOf(0x00))
        assertEquals(0L, reader.readVarint())
    }

    @Test
    fun testReadVarintOne() = runTest {
        val reader = byteReaderOf(byteArrayOf(0x01))
        assertEquals(1L, reader.readVarint())
    }

    @Test
    fun testReadVarintMaxOneByte() = runTest {
        val reader = byteReaderOf(byteArrayOf(0x7F))
        assertEquals(127L, reader.readVarint())
    }

    @Test
    fun testReadVarintMinTwoBytes() = runTest {
        val reader = byteReaderOf(byteArrayOf(0x80.toByte(), 0x01))
        assertEquals(128L, reader.readVarint())
    }

    @Test
    fun testReadVarintThreeHundred() = runTest {
        val reader = byteReaderOf(byteArrayOf(0xAC.toByte(), 0x02))
        assertEquals(300L, reader.readVarint())
    }

    @Test
    fun testReadVarintMaxTwoBytes() = runTest {
        val reader = byteReaderOf(byteArrayOf(0xFF.toByte(), 0x7F))
        assertEquals(16383L, reader.readVarint())
    }

    @Test
    fun testReadVarintLargeValue() = runTest {
        // 2147483647 = Int.MAX_VALUE = 0x7FFFFFFF
        // varint encoding: 0xFF 0xFF 0xFF 0xFF 0x07
        val reader = byteReaderOf(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x07))
        assertEquals(Int.MAX_VALUE.toLong(), reader.readVarint())
    }

    @Test
    fun testReadVarintMaxLong() = runTest {
        // Long.MAX_VALUE = 0x7FFFFFFFFFFFFFFF
        // varint encoding: 9 bytes
        val reader = byteReaderOf(byteArrayOf(
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
            0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x7F
        ))
        assertEquals(Long.MAX_VALUE, reader.readVarint())
    }

    @Test
    fun testReadVarintOverflowThrows() = runTest {
        // 10 bytes all with continuation bit set — overflows 64 bits
        val bytes = ByteArray(10) { 0x80.toByte() }
        val reader = byteReaderOf(bytes)
        assertFailsWith<IllegalArgumentException> { reader.readVarint() }
    }

    // --- readLine ---

    @Test
    fun testReadLineEmptyReader() = runTest {
        val reader = byteReaderOf()
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineSingleLineWithLf() = runTest {
        val reader = byteReaderOf("hello\n".encodeToByteArray())
        assertEquals("hello", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineSingleLineWithCrlf() = runTest {
        val reader = byteReaderOf("hello\r\n".encodeToByteArray())
        assertEquals("hello", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineNoTrailingNewline() = runTest {
        val reader = byteReaderOf("hello".encodeToByteArray())
        assertEquals("hello", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineEmptyLine() = runTest {
        val reader = byteReaderOf("\n".encodeToByteArray())
        assertEquals("", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineEmptyLineCrlf() = runTest {
        val reader = byteReaderOf("\r\n".encodeToByteArray())
        assertEquals("", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineMultipleLines() = runTest {
        val reader = byteReaderOf("alpha\nbeta\ngamma".encodeToByteArray())
        assertEquals("alpha", reader.readLine())
        assertEquals("beta", reader.readLine())
        assertEquals("gamma", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    @Test
    fun testReadLineMultipleLinesCrlf() = runTest {
        val reader = byteReaderOf("alpha\r\nbeta\r\ngamma\r\n".encodeToByteArray())
        assertEquals("alpha", reader.readLine())
        assertEquals("beta", reader.readLine())
        assertEquals("gamma", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    // \n split across chunk boundary: "hello" | "\nworld"
    @Test
    fun testReadLineLfAcrossChunks() = runTest {
        val reader = byteReaderOf(
            "hello".encodeToByteArray(),
            "\nworld".encodeToByteArray(),
        )
        assertEquals("hello", reader.readLine())
        assertEquals("world", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    // \r\n split across chunk boundary: "hello\r" | "\nworld"
    @Test
    fun testReadLineCrLfSplitAcrossChunks() = runTest {
        val reader = byteReaderOf(
            "hello\r".encodeToByteArray(),
            "\nworld".encodeToByteArray(),
        )
        assertEquals("hello", reader.readLine())
        assertEquals("world", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    // Multi-byte char (é = 0xC3 0xA9) split across chunk boundary within a line
    @Test
    fun testReadLineMultiByteCharSplitAcrossChunks() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0x61, 0xC3.toByte()),           // 'a', lead byte of é
            byteArrayOf(0xA9.toByte(), 0x62, 0x0A),    // trail byte of é, 'b', '\n'
        )
        assertEquals("aéb", reader.readLine())
        assertEquals(null, reader.readLine())
    }

    // 4-byte char (😀 = 0xF0 0x9F 0x98 0x80) split across three chunks, terminated by \n
    @Test
    fun testReadLineFourByteCharAcrossThreeChunks() = runTest {
        val reader = byteReaderOf(
            byteArrayOf(0xF0.toByte(), 0x9F.toByte()),  // first 2 bytes of 😀
            byteArrayOf(0x98.toByte(), 0x80.toByte()),  // last 2 bytes of 😀
            byteArrayOf(0x0A),                           // '\n'
        )
        assertEquals("😀", reader.readLine())
        assertEquals(null, reader.readLine())
    }

// --- FileStorage.readText ---
// Use readerOf to supply the raw bytes, write them to a real file, then assert readText decodes correctly.

    @Test
    fun testReadTextTwoByteChar() = runTest {
        // é (U+00E9) = 0xC3 0xA9 — split across two chunks in the reader
        val chunks = arrayOf(byteArrayOf(0x61, 0xC3.toByte()), byteArrayOf(0xA9.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes("$path/text.bin", bytes, append = false)
            assertEquals("aéb", storage.readText("$path/text.bin"))
        }
    }

    @Test
    fun testReadTextThreeByteChar() = runTest {
        // € (U+20AC) = 0xE2 0x82 0xAC — split 1+2
        val chunks = arrayOf(byteArrayOf(0x61, 0xE2.toByte()), byteArrayOf(0x82.toByte(), 0xAC.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes("$path/text.bin", bytes, append = false)
            assertEquals("a€b", storage.readText("$path/text.bin"))
        }
    }

    @Test
    fun testReadTextFourByteCharAcrossThreeChunks() = runTest {
        // 😀 (U+1F600) = 0xF0 0x9F 0x98 0x80 — split across 3 chunks
        val chunks = arrayOf(byteArrayOf(0x61, 0xF0.toByte()), byteArrayOf(0x9F.toByte(), 0x98.toByte()), byteArrayOf(0x80.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes("$path/text.bin", bytes, append = false)
            assertEquals("a😀b", storage.readText("$path/text.bin"))
        }
    }
// --- FileStorage.readBytes ---

    @Test
    fun testReadBytesRoundtrip() = runTest {
        val chunks = arrayOf(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes("$path/bytes.bin", bytes, append = false)
            assertContentEquals(bytes, storage.readBytes("$path/bytes.bin"))
        }
    }

    @Test
    fun testReadBytesAcrossChunks() = runTest {
        val chunks = arrayOf(byteArrayOf(0x01, 0x02), byteArrayOf(0x03, 0x04))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes("$path/bytes.bin", bytes, append = false)
            assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), storage.readBytes("$path/bytes.bin"))
        }
    }
}