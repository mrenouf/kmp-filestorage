package com.bitgrind.filestorage

import com.bitgrind.filestorage.testing.byteReaderOf
import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

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
    }
// --- FileStorage.readText ---
// Use readerOf to supply the raw bytes, write them to a real file, then assert readText decodes correctly.

    @Test
    fun testReadTextTwoByteChar() = runTest {
        // é (U+00E9) = 0xC3 0xA9 — split across two chunks in the reader
        val chunks = arrayOf(byteArrayOf(0x61, 0xC3.toByte()), byteArrayOf(0xA9.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes(path("text.bin"), bytes, append = false)
            assertEquals("aéb", storage.readText(path("text.bin")))
        }
    }

    @Test
    fun testReadTextThreeByteChar() = runTest {
        // € (U+20AC) = 0xE2 0x82 0xAC — split 1+2
        val chunks = arrayOf(byteArrayOf(0x61, 0xE2.toByte()), byteArrayOf(0x82.toByte(), 0xAC.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes(path("text.bin"), bytes, append = false)
            assertEquals("a€b", storage.readText(path("text.bin")))
        }
    }

    @Test
    fun testReadTextFourByteCharAcrossThreeChunks() = runTest {
        // 😀 (U+1F600) = 0xF0 0x9F 0x98 0x80 — split across 3 chunks
        val chunks = arrayOf(byteArrayOf(0x61, 0xF0.toByte()), byteArrayOf(0x9F.toByte(), 0x98.toByte()), byteArrayOf(0x80.toByte(), 0x62))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes(path("text.bin"), bytes, append = false)
            assertEquals("a😀b", storage.readText(path("text.bin")))
        }
    }
// --- FileStorage.readBytes ---

    @Test
    fun testReadBytesRoundtrip() = runTest {
        val chunks = arrayOf(byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte()))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes(path("bytes.bin"), bytes, append = false)
            assertContentEquals(bytes, storage.readBytes(path("bytes.bin")))
        }
    }

    @Test
    fun testReadBytesAcrossChunks() = runTest {
        val chunks = arrayOf(byteArrayOf(0x01, 0x02), byteArrayOf(0x03, 0x04))
        runFileStorageTest { storage, path ->
            val bytes = byteReaderOf(*chunks).readByteArray(chunks.sumOf { it.size })
            storage.writeBytes(path("bytes.bin"), bytes, append = false)
            assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), storage.readBytes(path("bytes.bin")))
        }
    }
}