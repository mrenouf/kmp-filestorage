package com.bitgrind.filestorage

import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.*


class FileStorageTest {
    // --- write/read roundtrip ---
    @Test
    fun testWriteAndReadByte() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("byte.bin"), append = false)
            writer.writeByte(0x12.toByte())
            writer.close()

            val reader = storage.getReader(path("byte.bin"))
            assertEquals(0x12.toByte(), reader.readByte())
            reader.close()
        }
    }
    @Test
    fun testWriteAndReadShort() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("short.bin"), append = false)
            writer.writeShort(0x1234.toShort())
            writer.close()

            val reader = storage.getReader(path("short.bin"))
            assertEquals(0x1234.toShort(), reader.readShort())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("int.bin"), append = false)
            writer.writeInt(0x12345678)
            writer.close()

            val reader = storage.getReader(path("int.bin"))
            assertEquals(0x12345678, reader.readInt())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadString() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("string.bin"), append = false)
            writer.writeString("hello world")
            writer.close()

            val reader = storage.getReader(path("string.bin"))
            assertEquals("hello world", reader.readString(11))
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadEmptyString() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("empty.bin"), "", append = false)
            assertEquals("", storage.readText(path("empty.bin")))
        }
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        runFileStorageTest { storage, path ->
            val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
            storage.writeBytes(path("bytes.bin"), payload, append = false)
            assertContentEquals(payload, storage.readBytes(path("bytes.bin")))
        }
    }

    @Test
    fun testWriteAndReadEmptyByteArray() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeBytes(path("empty-bytes.bin"), byteArrayOf(), append = false)
            assertContentEquals(byteArrayOf(), storage.readBytes(path("empty-bytes.bin")))
        }
    }

    @Test
    fun testWriteByteArrayWithOffset() = runTest {
        runFileStorageTest { storage, path ->
            // Source array: [0x00, 0xAA, 0xBB, 0xCC, 0xFF] — write only the middle 3 bytes
            val source = byteArrayOf(0x00, 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xFF.toByte())
            val writer = storage.getWriter(path("offset.bin"), append = false)
            writer.writeByteArray(source, offset = 1, length = 3)
            writer.close()

            assertContentEquals(
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()),
                storage.readBytes(path("offset.bin"))
            )
        }
    }

    @Test
    fun testWriteAndReadMultipleValues() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("multi.bin"), append = false)
            writer.writeShort(300.toShort())
            writer.writeInt(0xCAFEBABE.toInt())
            writer.writeString("test")
            writer.writeByteArray(byteArrayOf(1, 2, 3))
            writer.close()

            val reader = storage.getReader(path("multi.bin"))
            assertEquals(300.toShort(), reader.readShort())
            assertEquals(0xCAFEBABE.toInt(), reader.readInt())
            assertEquals("test", reader.readString(4))
            assertContentEquals(byteArrayOf(1, 2, 3), reader.readByteArray(3))
            reader.close()
        }
    }
// --- readText / writeText ---

    @Test
    fun testWriteAndReadText() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("text.txt"), "hello, world", append = false)
            assertEquals("hello, world", storage.readText(path("text.txt")))
        }
    }

    @Test
    fun testWriteTextOverwrites() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("overwrite.txt"), "first", append = false)
            storage.writeText(path("overwrite.txt"), "second", append = false)
            assertEquals("second", storage.readText(path("overwrite.txt")))
        }
    }

    @Test
    fun testWriteTextAppend() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("append.txt"), "hello", append = false)
            storage.writeText(path("append.txt"), " world", append = true)
            assertEquals("hello world", storage.readText(path("append.txt")))
        }
    }

    @Test
    fun testWriteAndReadEmptyText() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("empty.txt"), "", append = false)
            assertEquals("", storage.readText(path("empty.txt")))
        }
    }

    @Test
    fun testWriteAndReadMultilineText() = runTest {
        runFileStorageTest { storage, path ->
            val content = "line one\nline two\nline three"
            storage.writeText(path("multiline.txt"), content, append = false)
            assertEquals(content, storage.readText(path("multiline.txt")))
        }
    }

    @Test
    fun testWriteAndReadUnicodeText() = runTest {
        runFileStorageTest { storage, path ->
            val content = "こんにちは 🌍"
            storage.writeText(path("unicode.txt"), content, append = false)
            assertEquals(content, storage.readText(path("unicode.txt")))
        }
    }
// --- numeric boundaries ---

    @Test
    fun testSignedBoundaryValues() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("signed-boundaries.bin"), append = false)
            writer.writeByte(Byte.MIN_VALUE)
            writer.writeByte(Byte.MAX_VALUE)
            writer.writeShort(Short.MIN_VALUE)
            writer.writeShort(Short.MAX_VALUE)
            writer.writeInt(Int.MIN_VALUE)
            writer.writeInt(Int.MAX_VALUE)
            writer.writeLong(Long.MIN_VALUE)
            writer.writeLong(Long.MAX_VALUE)
            writer.close()

            val reader = storage.getReader(path("signed-boundaries.bin"))
            assertEquals(Byte.MIN_VALUE, reader.readByte())
            assertEquals(Byte.MAX_VALUE, reader.readByte())
            assertEquals(Short.MIN_VALUE, reader.readShort())
            assertEquals(Short.MAX_VALUE, reader.readShort())
            assertEquals(Int.MIN_VALUE, reader.readInt())
            assertEquals(Int.MAX_VALUE, reader.readInt())
            assertEquals(Long.MIN_VALUE, reader.readLong())
            assertEquals(Long.MAX_VALUE, reader.readLong())
            reader.close()
        }
    }

    @Test
    fun testUnsignedMaxValuesRoundTrip() = runTest {
        // Unsigned types share the same bit pattern as signed — MAX_VALUE for each
        // unsigned type is -1 in the corresponding signed type. This test verifies
        // the wire format preserves every bit.
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("unsigned-max.bin"), append = false)
            writer.writeByte(UByte.MAX_VALUE.toByte())    // 0xFF
            writer.writeShort(UShort.MAX_VALUE.toShort()) // 0xFFFF
            writer.writeInt(UInt.MAX_VALUE.toInt())       // 0xFFFFFFFF
            writer.writeLong(ULong.MAX_VALUE.toLong())    // 0xFFFFFFFFFFFFFFFF
            writer.close()

            val reader = storage.getReader(path("unsigned-max.bin"))
            assertEquals(UByte.MAX_VALUE, reader.readByte().toUByte())
            assertEquals(UShort.MAX_VALUE, reader.readShort().toUShort())
            assertEquals(UInt.MAX_VALUE, reader.readInt().toUInt())
            assertEquals(ULong.MAX_VALUE, reader.readLong().toULong())
            reader.close()
        }
    }

    @Test
    fun testByteArrayBoundaryValues() = runTest {
        // Exercises 0x00, 0xFF, 0x80 (Byte.MIN_VALUE), 0x7F (Byte.MAX_VALUE) —
        // the four byte values that most commonly expose sign-extension bugs.
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("byte-boundaries.bin"), append = false)
            writer.writeByteArray(byteArrayOf(0x00, 0xFF.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE))
            writer.close()

            val reader = storage.getReader(path("byte-boundaries.bin"))
            assertContentEquals(
                byteArrayOf(0x00, 0xFF.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE),
                reader.readByteArray(4)
            )
            reader.close()
        }
    }

// --- big-endian byte order ---

    @Test
    fun testShortBigEndian() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("be-short.bin"), append = false)
            writer.writeShort(0x0102.toShort())
            writer.close()

            val reader = storage.getReader(path("be-short.bin"))
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
        }
    }

    @Test
    fun testIntBigEndian() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("be-int.bin"), append = false)
            writer.writeInt(0x01020304)
            writer.close()

            val reader = storage.getReader(path("be-int.bin"))
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
            assertEquals(0x03.toByte(), reader.readByte())
            assertEquals(0x04.toByte(), reader.readByte())
        }
    }

    @Test
    fun testWriteAndReadLong() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("long.bin"), append = false)
            writer.writeLong(0x0102030405060708L)
            writer.close()

            val reader = storage.getReader(path("long.bin"))
            assertEquals(0x0102030405060708L, reader.readLong())
            reader.close()
        }
    }

    @Test
    fun testLongBigEndian() = runTest {
        runFileStorageTest { storage, path ->
            val writer = storage.getWriter(path("be-long.bin"), append = false)
            writer.writeLong(0x0102030405060708L)
            writer.close()

            val reader = storage.getReader(path("be-long.bin"))
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
            assertEquals(0x03.toByte(), reader.readByte())
            assertEquals(0x04.toByte(), reader.readByte())
            assertEquals(0x05.toByte(), reader.readByte())
            assertEquals(0x06.toByte(), reader.readByte())
            assertEquals(0x07.toByte(), reader.readByte())
            assertEquals(0x08.toByte(), reader.readByte())
        }
    }
// --- append mode ---

    @Test
    fun testAppendMode() = runTest {
        runFileStorageTest { storage, path ->
            val writer1 = storage.getWriter(path("append.bin"), append = false)
            writer1.writeShort(1.toShort())
            writer1.close()

            val writer2 = storage.getWriter(path("append.bin"), append = true)
            writer2.writeShort(2.toShort())
            writer2.close()

            val reader = storage.getReader(path("append.bin"))
            assertEquals(1.toShort(), reader.readShort())
            assertEquals(2.toShort(), reader.readShort())
            reader.close()
        }
    }
// --- createDirectories ---

    @Test
    fun testCreateDirectories() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("a/b/c"))
            assertTrue(storage.exists(path("a/b/c")))
        }
    }

    @Test
    fun testCreateDirectoriesIdempotent() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("dir"))
            storage.createDirectories(path("dir"))
            assertTrue(storage.exists(path("dir")))
        }
    }
// --- exists ---

    @Test
    fun testExistsReturnsFalse() = runTest {
        runFileStorageTest { storage, path ->
            assertFalse(storage.exists(path("does-not-exist")))
        }
    }

// --- delete ---

    @Test
    fun testDeleteFile() = runTest {
        runFileStorageTest { storage, path ->
            storage.getWriter(path("file.bin"), append = false).close()
            assertTrue(storage.exists(path("file.bin")))
            storage.delete(path("file.bin"), recursive = false)
            assertFalse(storage.exists(path("file.bin")))
        }
    }

    @Test
    fun testDeleteEmptyDirectory() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("empty"))
            storage.delete(path("empty"), recursive = false)
            assertFalse(storage.exists(path("empty")))
        }
    }

    @Test
    fun testDeleteDirectoryRecursively() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("tree/a"))
            storage.getWriter(path("tree/a/b.txt"), append = false).close()
            storage.getWriter(path("tree/c.txt"), append = false).close()

            storage.delete(path("tree"), recursive = true)
            assertFalse(storage.exists(path("tree")))
        }
    }

    @Test
    fun testDeleteNonExistentThrows() = runTest {
        runFileStorageTest { storage, path ->
            assertFails {
                storage.delete(path("does-not-exist"), recursive = false)
            }
        }
    }
// --- EOF / error paths ---

    @Test
    fun testReadByteThrowsOnEof() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeBytes(path("empty.bin"), byteArrayOf(), append = false)
            val reader = storage.getReader(path("empty.bin"))
            assertFailsWith<IOException> { reader.readByte() }
            reader.close()
        }
    }

    @Test
    fun testReadIntThrowsOnEof() = runTest {
        runFileStorageTest { storage, path ->
            // Write only 1 byte; reading an Int (4 bytes) must throw
            val writer = storage.getWriter(path("truncated.bin"), append = false)
            writer.writeByte(0x01)
            writer.close()

            val reader = storage.getReader(path("truncated.bin"))
            assertFailsWith<IOException> { reader.readInt() }
            reader.close()
        }
    }

    @Test
    fun testReadByteArrayThrowsOnEof() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeBytes(path("short.bin"), byteArrayOf(0x01, 0x02), append = false)
            val reader = storage.getReader(path("short.bin"))
            assertFailsWith<IOException> { reader.readByteArray(5) }
            reader.close()
        }
    }

    @Test
    fun testReadStringThrowsOnEof() = runTest {
        runFileStorageTest { storage, path ->
            // "ab" is 2 UTF-8 bytes; requesting 5 must throw
            storage.writeText(path("short.txt"), "ab", append = false)
            val reader = storage.getReader(path("short.txt"))
            assertFailsWith<IOException> { reader.readString(5) }
            reader.close()
        }
    }
// --- zero-length reads ---

    @Test
    fun testReadStringZeroLength() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeText(path("data.txt"), "hello", append = false)
            val reader = storage.getReader(path("data.txt"))
            assertEquals("", reader.readString(0))
            reader.close()
        }
    }

    @Test
    fun testReadByteArrayZeroLength() = runTest {
        runFileStorageTest { storage, path ->
            storage.writeBytes(path("data.bin"), byteArrayOf(0x01, 0x02), append = false)
            val reader = storage.getReader(path("data.bin"))
            assertContentEquals(byteArrayOf(), reader.readByteArray(0))
            reader.close()
        }
    }
// --- createTempDir / createTempFile ---

    @Test
    fun testCreateTempDirIsUnique() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("tmp"))
            val dir1 = storage.createTempDir(path = path("tmp"))
            val dir2 = storage.createTempDir(path = path("tmp"))
            assertNotEquals(dir1, dir2)
        }
    }

    @Test
    fun testCreateTempFileHasPrefixAndSuffix() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("tmp"))
            val file = storage.createTempFile(path = path("tmp"), prefix = "myprefix-", suffix = ".bin")
            val name = file.substringAfterLast('/')
            assertTrue(name.startsWith("myprefix-"))
            assertTrue(name.endsWith(".bin"))
        }
    }

    @Test
    fun testCreateTempFileIsUnique() = runTest {
        runFileStorageTest { storage, path ->
            storage.createDirectories(path("tmp"))
            val file1 = storage.createTempFile(path = path("tmp"))
            val file2 = storage.createTempFile(path = path("tmp"))
            assertNotEquals(file1, file2)
        }
    }

    @Test
    fun testCreateTempDirDefaultPath() = runTest {
        runFileStorageTest { storage, _ ->
            val dir = storage.createTempDir()
            try {
                assertTrue(storage.exists(dir))
            } finally {
                storage.delete(dir, recursive = true)
            }
        }
    }

    @Test
    fun testCreateTempFileDefaultPath() = runTest {
        runFileStorageTest { storage, _ ->
            val file = storage.createTempFile()
            try {
                assertTrue(storage.exists(file))
            } finally {
                storage.delete(file, recursive = false)
            }
        }
    }

    @Test
    fun testCreateTempDirIllegalPrefix() = runTest {
        runFileStorageTest { storage, path ->
            assertFailsWith<IllegalArgumentException> {
                storage.createTempDir(path = path("tmp"), prefix = "bad/prefix")
            }
        }
    }

    @Test
    fun testCreateTempFileIllegalSuffix() = runTest {
        runFileStorageTest { storage, path ->
            assertFailsWith<IllegalArgumentException> {
                storage.createTempFile(path = path("tmp"), suffix = "bad/suffix")
            }
        }
    }
}
