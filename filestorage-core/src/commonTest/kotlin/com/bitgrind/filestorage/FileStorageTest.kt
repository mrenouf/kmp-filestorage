package com.bitgrind.filestorage

import com.bitgrind.filestorage.internal.FileStorageTestApi
import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.random.Random
import kotlin.test.*


class FileStorageTest {
    // --- write/read roundtrip ---
    @Test
    fun testWriteAndReadByte() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/byte.bin", append = false)
            writer.writeByte(0x12.toByte())
            writer.close()

            val reader = storage.getReader("$basePath/byte.bin")
            assertEquals(0x12.toByte(), reader.readByte())
            reader.close()
        }
    }
    @Test
    fun testWriteAndReadShort() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/short.bin", append = false)
            writer.writeShort(0x1234.toShort())
            writer.close()

            val reader = storage.getReader("$basePath/short.bin")
            assertEquals(0x1234.toShort(), reader.readShort())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/int.bin", append = false)
            writer.writeInt(0x12345678)
            writer.close()

            val reader = storage.getReader("$basePath/int.bin")
            assertEquals(0x12345678, reader.readInt())
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadString() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/string.bin", append = false)
            writer.writeString("hello world")
            writer.close()

            val reader = storage.getReader("$basePath/string.bin")
            assertEquals("hello world", reader.readString(11))
            reader.close()
        }
    }

    @Test
    fun testWriteAndReadEmptyString() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/empty.bin", "", append = false)
            assertEquals("", storage.readText("$basePath/empty.bin"))
        }
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        runFileStorageTest { storage, basePath ->
            val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
            storage.writeBytes("$basePath/bytes.bin", payload, append = false)
            assertContentEquals(payload, storage.readBytes("$basePath/bytes.bin"))
        }
    }

    @Test
    fun testWriteAndReadEmptyByteArray() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeBytes("$basePath/empty-bytes.bin", byteArrayOf(), append = false)
            assertContentEquals(byteArrayOf(), storage.readBytes("$basePath/empty-bytes.bin"))
        }
    }

    @Test
    fun testWriteByteArrayWithOffset() = runTest {
        runFileStorageTest { storage, basePath ->
            // Source array: [0x00, 0xAA, 0xBB, 0xCC, 0xFF] — write only the middle 3 bytes
            val source = byteArrayOf(0x00, 0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xFF.toByte())
            val writer = storage.getWriter("$basePath/offset.bin", append = false)
            writer.writeByteArray(source, offset = 1, length = 3)
            writer.close()

            assertContentEquals(
                byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()),
                storage.readBytes("$basePath/offset.bin")
            )
        }
    }

    @Test
    fun testWriteAndReadMultipleValues() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/multi.bin", append = false)
            writer.writeShort(300.toShort())
            writer.writeInt(0xCAFEBABE.toInt())
            writer.writeString("test")
            writer.writeByteArray(byteArrayOf(1, 2, 3))
            writer.close()

            val reader = storage.getReader("$basePath/multi.bin")
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
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/text.txt", "hello, world", append = false)
            assertEquals("hello, world", storage.readText("$basePath/text.txt"))
        }
    }

    @Test
    fun testWriteTextOverwrites() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/overwrite.txt", "first", append = false)
            storage.writeText("$basePath/overwrite.txt", "second", append = false)
            assertEquals("second", storage.readText("$basePath/overwrite.txt"))
        }
    }

    @Test
    fun testWriteTextAppend() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/append.txt", "hello", append = false)
            storage.writeText("$basePath/append.txt", " world", append = true)
            assertEquals("hello world", storage.readText("$basePath/append.txt"))
        }
    }

    @Test
    fun testWriteAndReadEmptyText() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/empty.txt", "", append = false)
            assertEquals("", storage.readText("$basePath/empty.txt"))
        }
    }

    @Test
    fun testWriteAndReadMultilineText() = runTest {
        runFileStorageTest { storage, basePath ->
            val content = "line one\nline two\nline three"
            storage.writeText("$basePath/multiline.txt", content, append = false)
            assertEquals(content, storage.readText("$basePath/multiline.txt"))
        }
    }

    @Test
    fun testWriteAndReadUnicodeText() = runTest {
        runFileStorageTest { storage, basePath ->
            val content = "こんにちは 🌍"
            storage.writeText("$basePath/unicode.txt", content, append = false)
            assertEquals(content, storage.readText("$basePath/unicode.txt"))
        }
    }
// --- numeric boundaries ---

    @Test
    fun testSignedBoundaryValues() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/signed-boundaries.bin", append = false)
            writer.writeByte(Byte.MIN_VALUE)
            writer.writeByte(Byte.MAX_VALUE)
            writer.writeShort(Short.MIN_VALUE)
            writer.writeShort(Short.MAX_VALUE)
            writer.writeInt(Int.MIN_VALUE)
            writer.writeInt(Int.MAX_VALUE)
            writer.writeLong(Long.MIN_VALUE)
            writer.writeLong(Long.MAX_VALUE)
            writer.close()

            val reader = storage.getReader("$basePath/signed-boundaries.bin")
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
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/unsigned-max.bin", append = false)
            writer.writeByte(UByte.MAX_VALUE.toByte())    // 0xFF
            writer.writeShort(UShort.MAX_VALUE.toShort()) // 0xFFFF
            writer.writeInt(UInt.MAX_VALUE.toInt())       // 0xFFFFFFFF
            writer.writeLong(ULong.MAX_VALUE.toLong())    // 0xFFFFFFFFFFFFFFFF
            writer.close()

            val reader = storage.getReader("$basePath/unsigned-max.bin")
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
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/byte-boundaries.bin", append = false)
            writer.writeByteArray(byteArrayOf(0x00, 0xFF.toByte(), Byte.MIN_VALUE, Byte.MAX_VALUE))
            writer.close()

            val reader = storage.getReader("$basePath/byte-boundaries.bin")
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
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/be-short.bin", append = false)
            writer.writeShort(0x0102.toShort())
            writer.close()

            val reader = storage.getReader("$basePath/be-short.bin")
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
        }
    }

    @Test
    fun testIntBigEndian() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/be-int.bin", append = false)
            writer.writeInt(0x01020304)
            writer.close()

            val reader = storage.getReader("$basePath/be-int.bin")
            assertEquals(0x01.toByte(), reader.readByte())
            assertEquals(0x02.toByte(), reader.readByte())
            assertEquals(0x03.toByte(), reader.readByte())
            assertEquals(0x04.toByte(), reader.readByte())
        }
    }

    @Test
    fun testWriteAndReadLong() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/long.bin", append = false)
            writer.writeLong(0x0102030405060708L)
            writer.close()

            val reader = storage.getReader("$basePath/long.bin")
            assertEquals(0x0102030405060708L, reader.readLong())
            reader.close()
        }
    }

    @Test
    fun testLongBigEndian() = runTest {
        runFileStorageTest { storage, basePath ->
            val writer = storage.getWriter("$basePath/be-long.bin", append = false)
            writer.writeLong(0x0102030405060708L)
            writer.close()

            val reader = storage.getReader("$basePath/be-long.bin")
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
        runFileStorageTest { storage, basePath ->
            val writer1 = storage.getWriter("$basePath/append.bin", append = false)
            writer1.writeShort(1.toShort())
            writer1.close()

            val writer2 = storage.getWriter("$basePath/append.bin", append = true)
            writer2.writeShort(2.toShort())
            writer2.close()

            val reader = storage.getReader("$basePath/append.bin")
            assertEquals(1.toShort(), reader.readShort())
            assertEquals(2.toShort(), reader.readShort())
            reader.close()
        }
    }
// --- createDirectories ---

    @Test
    fun testCreateDirectories() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/a/b/c")
            assertTrue(storage.exists("$basePath/a/b/c"))
        }
    }

    @Test
    fun testCreateDirectoriesIdempotent() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/dir")
            storage.createDirectories("$basePath/dir")
            assertTrue(storage.exists("$basePath/dir"))
        }
    }

// --- exists ---

    @Test
    fun testExistsReturnsFalse() = runTest {
        runFileStorageTest { storage, basePath ->
            assertFalse(storage.exists("$basePath/does-not-exist"))
        }
    }

    @Test
    fun testExistsReturnsTrueForExistingDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            assertTrue(storage.exists(basePath))
        }
    }

// --- delete ---

    @Test
    fun testDeleteFile() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.getWriter("$basePath/file.bin", append = false).close()
            assertTrue(storage.exists("$basePath/file.bin"))
            storage.delete("$basePath/file.bin", recursive = false)
            assertFalse(storage.exists("$basePath/file.bin"))
        }
    }

    @Test
    fun testDeleteEmptyDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/empty")
            storage.delete("$basePath/empty", recursive = false)
            assertFalse(storage.exists("$basePath/empty"))
        }
    }

    @Test
    fun testDeleteDirectoryRecursively() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/tree/a")
            storage.getWriter("$basePath/tree/a/b.txt", append = false).close()
            storage.getWriter("$basePath/tree/c.txt", append = false).close()

            storage.delete("$basePath/tree", recursive = true)
            assertFalse(storage.exists("$basePath/tree"))
        }
    }

    @Test
    fun testDeleteNonExistentThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            assertFails {
                storage.delete("$basePath/does-not-exist", recursive = false)
            }
        }
    }
// --- EOF / error basePaths ---

    @Test
    fun testReadByteThrowsOnEof() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeBytes("$basePath/empty.bin", byteArrayOf(), append = false)
            val reader = storage.getReader("$basePath/empty.bin")
            assertFailsWith<IOException> { reader.readByte() }
            reader.close()
        }
    }

    @Test
    fun testReadIntThrowsOnEof() = runTest {
        runFileStorageTest { storage, basePath ->
            // Write only 1 byte; reading an Int (4 bytes) must throw
            val writer = storage.getWriter("$basePath/truncated.bin", append = false)
            writer.writeByte(0x01)
            writer.close()

            val reader = storage.getReader("$basePath/truncated.bin")
            assertFailsWith<IOException> { reader.readInt() }
            reader.close()
        }
    }

    @Test
    fun testReadByteArrayThrowsOnEof() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeBytes("$basePath/short.bin", byteArrayOf(0x01, 0x02), append = false)
            val reader = storage.getReader("$basePath/short.bin")
            assertFailsWith<IOException> { reader.readByteArray(5) }
            reader.close()
        }
    }

    @Test
    fun testReadStringThrowsOnEof() = runTest {
        runFileStorageTest { storage, basePath ->
            // "ab" is 2 UTF-8 bytes; requesting 5 must throw
            storage.writeText("$basePath/short.txt", "ab", append = false)
            val reader = storage.getReader("$basePath/short.txt")
            assertFailsWith<IOException> { reader.readString(5) }
            reader.close()
        }
    }
// --- zero-length reads ---

    @Test
    fun testReadStringZeroLength() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/data.txt", "hello", append = false)
            val reader = storage.getReader("$basePath/data.txt")
            assertEquals("", reader.readString(0))
            reader.close()
        }
    }

    @Test
    fun testReadByteArrayZeroLength() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeBytes("$basePath/data.bin", byteArrayOf(0x01, 0x02), append = false)
            val reader = storage.getReader("$basePath/data.bin")
            assertContentEquals(byteArrayOf(), reader.readByteArray(0))
            reader.close()
        }
    }
// --- createTempDir / createTempFile ---

    @Test
    fun testCreateTempDirFailsWhenAlreadyExists() = runTest {
        runFileStorageTest { storage, basePath ->
            val seed = Random.nextInt()
            val expectedPath = storage.tempName(path = "$basePath/existing-dir", random = Random(seed))
            storage.createDirectories(expectedPath)
            (storage as FileStorageTestApi).random = Random(seed)

            assertFailsWith<IOException> {
                storage.createTempDir(path = "$basePath/existing-dir")
            }
        }
    }

    @Test
    fun testCreateTempFileHasPrefixAndSuffix() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/tmp")
            val file = storage.createTempFile(path = "$basePath/tmp", prefix = "myprefix-", suffix = ".bin")
            val name = file.substringAfterLast('/')
            assertTrue(name.startsWith("myprefix-"))
            assertTrue(name.endsWith(".bin"))
        }
    }

    @Test
    fun testCreateTempFailsWhenFileExists() = runTest {
        runFileStorageTest { storage, basePath ->
            val seed = Random.nextInt()
            val expectedPath = storage.tempName(path = "$basePath/tmp", random = Random(seed))
            storage.createDirectories(expectedPath)
            (storage as FileStorageTestApi).random = Random(seed)

            assertFailsWith<IOException> {
                storage.createTempFile(path = "$basePath/tmp")
            }
        }
    }

    @Test
    fun testCreateTempFileIsUnique() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/tmp")
            val file1 = storage.createTempFile(path = "$basePath/tmp")
            val file2 = storage.createTempFile(path = "$basePath/tmp")
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
    fun testCreateTempDirIsUnique() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/tmp")
            val dir1 = storage.createTempDir(path = "$basePath/tmp")
            val dir2 = storage.createTempDir(path = "$basePath/tmp")
            assertNotEquals(dir1, dir2)
        }
    }

    @Test
    fun testCreateTempFileCreatesDirectories() = runTest {
        runFileStorageTest { storage, basePath ->
            val tempFile = storage.createTempFile(path = "$basePath/new/tmp")
            assertTrue(storage.exists(tempFile))
        }
    }

    @Test
    fun testCreateTempDirIllegalPrefix() = runTest {
        runFileStorageTest { storage, basePath ->
            assertFailsWith<IllegalArgumentException> {
                storage.createTempDir(path = "$basePath/tmp", prefix = "bad/prefix")
            }
        }
    }

    @Test
    fun testCreateTempFileIllegalSuffix() = runTest {
        runFileStorageTest { storage, basePath ->
            assertFailsWith<IllegalArgumentException> {
                storage.createTempFile(path = "$basePath/tmp", suffix = "bad/suffix")
            }
        }
    }
}
