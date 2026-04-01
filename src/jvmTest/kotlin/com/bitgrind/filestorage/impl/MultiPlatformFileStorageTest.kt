package com.bitgrind.filestorage.impl

import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.test.*

@ExperimentalPathApi
class MultiPlatformFileStorageTest {

    private val tempDir = createTempDirectory("opfs-test")
    private val storage = MultiPlatformFileStorage()

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    private fun path(name: String): String = tempDir.resolve(name).toString()

    // --- write/read roundtrip ---

    @Test
    fun testWriteAndReadShort() = runTest {
        val writer = storage.getWriter(path("short.bin"), append = false)
        writer.writeShort(0x1234.toShort())
        writer.close()

        val reader = storage.getReader(path("short.bin"))
        assertEquals(0x1234.toShort(), reader.readShort())
        reader.close()
    }

    @Test
    fun testWriteAndReadInt() = runTest {
        val writer = storage.getWriter(path("int.bin"), append = false)
        writer.writeInt(0x12345678)
        writer.close()

        val reader = storage.getReader(path("int.bin"))
        assertEquals(0x12345678, reader.readInt())
        reader.close()
    }

    @Test
    fun testWriteAndReadString() = runTest {
        val writer = storage.getWriter(path("string.bin"), append = false)
        writer.writeString("hello world")
        writer.close()

        val reader = storage.getReader(path("string.bin"))
        assertEquals("hello world", reader.readString())
        reader.close()
    }

    @Test
    fun testWriteAndReadEmptyString() = runTest {
        val writer = storage.getWriter(path("empty.bin"), append = false)
        writer.writeString("")
        writer.close()

        val reader = storage.getReader(path("empty.bin"))
        assertEquals("", reader.readString())
        reader.close()
    }

    @Test
    fun testWriteAndReadByteArray() = runTest {
        val payload = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val writer = storage.getWriter(path("bytes.bin"), append = false)
        writer.writeByteArray(payload)
        writer.close()

        val reader = storage.getReader(path("bytes.bin"))
        assertContentEquals(payload, reader.readByteArray())
        reader.close()
    }

    @Test
    fun testWriteAndReadEmptyByteArray() = runTest {
        val writer = storage.getWriter(path("empty-bytes.bin"), append = false)
        writer.writeByteArray(byteArrayOf())
        writer.close()

        val reader = storage.getReader(path("empty-bytes.bin"))
        assertContentEquals(byteArrayOf(), reader.readByteArray())
        reader.close()
    }

    @Test
    fun testWriteAndReadMultipleValues() = runTest {
        val writer = storage.getWriter(path("multi.bin"), append = false)
        writer.writeShort(300.toShort())
        writer.writeInt(0xCAFEBABE.toInt())
        writer.writeString("test")
        writer.writeByteArray(byteArrayOf(1, 2, 3))
        writer.close()

        val reader = storage.getReader(path("multi.bin"))
        assertEquals(300.toShort(), reader.readShort())
        assertEquals(0xCAFEBABE.toInt(), reader.readInt())
        assertEquals("test", reader.readString())
        assertContentEquals(byteArrayOf(1, 2, 3), reader.readByteArray())
        reader.close()
    }

    // --- big-endian byte order ---

    @Test
    fun testShortBigEndian() = runTest {
        val writer = storage.getWriter(path("be-short.bin"), append = false)
        writer.writeShort(0x0102.toShort())
        writer.close()

        val bytes = File(path("be-short.bin")).readBytes()
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x02.toByte(), bytes[1])
    }

    @Test
    fun testIntBigEndian() = runTest {
        val writer = storage.getWriter(path("be-int.bin"), append = false)
        writer.writeInt(0x01020304)
        writer.close()

        val bytes = File(path("be-int.bin")).readBytes()
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(0x02.toByte(), bytes[1])
        assertEquals(0x03.toByte(), bytes[2])
        assertEquals(0x04.toByte(), bytes[3])
    }

    // --- append mode ---

    @Test
    fun testAppendMode() = runTest {
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

    // --- createDirectories ---

    @Test
    fun testCreateDirectories() = runTest {
        storage.createDirectories(path("a/b/c"))
        assertTrue(tempDir.resolve("a/b/c").toFile().isDirectory)
    }

    @Test
    fun testCreateDirectoriesIdempotent() = runTest {
        storage.createDirectories(path("dir"))
        storage.createDirectories(path("dir"))
        assertTrue(tempDir.resolve("dir").toFile().isDirectory)
    }

    // --- delete ---

    @Test
    fun testDeleteFile() = runTest {
        val file = tempDir.resolve("file.bin").toFile().also { it.createNewFile() }
        assertTrue(file.exists())
        storage.delete(file.absolutePath, recursive = false)
        assertFalse(file.exists())
    }

    @Test
    fun testDeleteEmptyDirectory() = runTest {
        val dir = tempDir.resolve("emptydir").toFile().also { it.mkdir() }
        assertTrue(dir.exists())
        storage.delete(dir.absolutePath, recursive = false)
        assertFalse(dir.exists())
    }

    @Test
    fun testDeleteDirectoryRecursively() = runTest {
        val dir = tempDir.resolve("tree").toFile().also { it.mkdirs() }
        dir.resolve("a").mkdirs()
        dir.resolve("a/b.txt").createNewFile()
        dir.resolve("c.txt").createNewFile()

        storage.delete(dir.absolutePath, recursive = true)
        assertFalse(dir.exists())
    }
}
