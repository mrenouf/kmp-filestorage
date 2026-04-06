package com.bitgrind.filestorage

import com.bitgrind.filestorage.test.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class FileStorageTest {
    // --- write/read roundtrip ---
    @Test
    fun testWriteAndReadByte() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("byte.bin", append = false)
            writer.writeByte(0x12.toByte())
            writer.close()

            val reader = storage.getReader("byte.bin")
            assertEquals(0x12.toByte(), reader.readByte())
            reader.close()
        }
    }
    @Test
    fun testWriteAndReadShort() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("short.bin", append = false)
            writer.writeShort(0x1234.toShort())
            writer.close()

            val reader = storage.getReader("short.bin")
            assertEquals(0x1234.toShort(), reader.readShort())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("int.bin", append = false)
            writer.writeInt(0x12345678)
            writer.close()

            val reader = storage.getReader("int.bin")
            assertEquals(0x12345678, reader.readInt())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadString() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("string.bin", append = false)
            writer.writeString("hello world")
            writer.close()

            val reader = storage.getReader("string.bin")
            assertEquals("hello world", reader.readString())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadEmptyString() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("empty.bin", append = false)
            writer.writeString("")
            writer.close()

            val reader = storage.getReader("empty.bin")
            assertEquals("", reader.readString())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        runFileStorageTest { storage ->
            val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
            val writer = storage.getWriter("bytes.bin", append = false)
            writer.writeByteArray(payload)
            writer.close()

            val reader = storage.getReader("bytes.bin")
            assertContentEquals(payload, reader.readByteArray())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadEmptyByteArray() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("empty-bytes.bin", append = false)
            writer.writeByteArray(byteArrayOf())
            writer.close()

            val reader = storage.getReader("empty-bytes.bin")
            assertContentEquals(byteArrayOf(), reader.readByteArray())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadMultipleValues() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("multi.bin", append = false)
            writer.writeShort(300.toShort())
            writer.writeInt(0xCAFEBABE.toInt())
            writer.writeString("test")
            writer.writeByteArray(byteArrayOf(1, 2, 3))
            writer.close()

            val reader = storage.getReader("multi.bin")
            assertEquals(300.toShort(), reader.readShort())
            assertEquals(0xCAFEBABE.toInt(), reader.readInt())
            assertEquals("test", reader.readString())
            assertContentEquals(byteArrayOf(1, 2, 3), reader.readByteArray())
            reader.close()
        }
    }
// --- readText / writeText ---

    @Test
    fun testWriteAndReadText() = runTest {
        runFileStorageTest { storage ->
            storage.writeText("text.txt", "hello, world", append = false)
            assertEquals("hello, world", storage.readText("text.txt"))
        }
    }

    @Test
    fun testWriteTextOverwrites() = runTest {
        runFileStorageTest { storage ->
            storage.writeText("overwrite.txt", "first", append = false)
            storage.writeText("overwrite.txt", "second", append = false)
            assertEquals("second", storage.readText("overwrite.txt"))
        }
    }

    @Test
    fun testWriteTextAppend() = runTest {
        runFileStorageTest { storage ->
            storage.writeText("append.txt", "hello", append = false)
            storage.writeText("append.txt", " world", append = true)
            assertEquals("hello world", storage.readText("append.txt"))
        }
    }

    @Test
    fun testWriteAndReadEmptyText() = runTest {
        runFileStorageTest { storage ->
            storage.writeText("empty.txt", "", append = false)
            assertEquals("", storage.readText("empty.txt"))
        }
    }

    @Test
    fun testWriteAndReadMultilineText() = runTest {
        runFileStorageTest { storage ->
            val content = "line one\nline two\nline three"
            storage.writeText("multiline.txt", content, append = false)
            assertEquals(content, storage.readText("multiline.txt"))
        }
    }

    @Test
    fun testWriteAndReadUnicodeText() = runTest {
        runFileStorageTest { storage ->
            val content = "こんにちは 🌍"
            storage.writeText("unicode.txt", content, append = false)
            assertEquals(content, storage.readText("unicode.txt"))
        }
    }
// --- big-endian byte order ---

    @Test
    fun testShortBigEndian() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("be-short.bin", append = false)
            writer.writeShort(0x0102.toShort())
            writer.close()

            val reader = storage.getReader("be-short.bin")
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
        }
    }

    @Test
    fun testIntBigEndian() = runTest {
        runFileStorageTest { storage ->
            val writer = storage.getWriter("be-int.bin", append = false)
            writer.writeInt(0x01020304)
            writer.close()

            val reader = storage.getReader("be-int.bin")
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
            assertEquals(0x03.toByte(), reader.readByte())
            assertEquals(0x04.toByte(), reader.readByte())
        }
    }
// --- append mode ---

    @Test
    fun testAppendMode() = runTest {
        runFileStorageTest { storage ->
            val writer1 = storage.getWriter("append.bin", append = false)
            writer1.writeShort(1.toShort())
            writer1.close()

            val writer2 = storage.getWriter("append.bin", append = true)
            writer2.writeShort(2.toShort())
            writer2.close()

            val reader = storage.getReader("append.bin")
            assertEquals(1.toShort(), reader.readShort())
            assertEquals(2.toShort(), reader.readShort())
            reader.close()
        }
    }
// --- createDirectories ---

    @Test
    fun testCreateDirectories() = runTest {
        runFileStorageTest { storage ->
            storage.createDirectories("a/b/c")
            assertTrue(storage.exists("a/b/c"))
        }
    }

    @Test
    fun testCreateDirectoriesIdempotent() = runTest {
        runFileStorageTest { storage ->
            storage.createDirectories("dir")
            storage.createDirectories("dir")
            assertTrue(storage.exists("dir"))
        }
    }
// --- delete ---

    @Test
    fun testDeleteFile() = runTest {
        runFileStorageTest { storage ->
            storage.getWriter("file.bin", append = false).close()
            assertTrue(storage.exists("file.bin"))
            storage.delete("file.bin", recursive = false)
            assertFalse(storage.exists("file.bin"))
        }
    }

    @Test
    fun testDeleteEmptyDirectory() = runTest {
        runFileStorageTest { storage ->
            storage.createDirectories("empty")
            storage.delete("empty", recursive = false)
            assertFalse(storage.exists("empty"))
        }
    }

    @Test
    fun testDeleteDirectoryRecursively() = runTest {
        runFileStorageTest { storage ->
            storage.createDirectories("tree/a")
            storage.getWriter("tree/a/b.txt", append = false).close()
            storage.getWriter("tree/c.txt", append = false).close()

            storage.delete("tree", recursive = true)
            assertFalse(storage.exists("tree"))
        }
    }
}