@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import web.fs.removeEntry
import web.navigator.navigator
import web.storage.getDirectory
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.*

class OpfsFileStorageTest {

    /**
     * Obtains the OPFS root, runs [block] with a scoped [OpfsFileStorage], then
     * recursively removes all entries created during the test.
     * Skips silently if OPFS is not available in the current environment.
     */
    private suspend fun runOpfsTest(block: suspend (OpfsFileStorage) -> Unit) {
        val root = try {
            navigator.storage.getDirectory()
        } catch (_: Exception) {
            println("OPFS not available — skipping")
            return
        }

        val storage = OpfsFileStorage(scope = CoroutineScope(currentCoroutineContext()))
        try {
            block(storage)
        } finally {
            root.values().asFlow().collect { entry ->
                root.removeEntry(entry.name, fileSystemRemoveOptions(recursive = true))
            }
        }
    }

    // --- write/read roundtrip ---

    @Test
    fun testWriteAndReadShort() = runTest {
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
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

    // --- append mode ---

    @Test
    fun testAppendMode() = runTest {
        runOpfsTest { storage ->
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
        runOpfsTest { storage ->
            storage.createDirectories("a/b/c")

            // Verify by writing a file inside the created path
            val writer = storage.getWriter("a/b/c/probe.bin", append = false)
            writer.writeShort(0.toShort())
            writer.close()
        }
    }

    @Test
    fun testCreateDirectoriesIdempotent() = runTest {
        runOpfsTest { storage ->
            storage.createDirectories("dir")
            storage.createDirectories("dir")

            val writer = storage.getWriter("dir/probe.bin", append = false)
            writer.writeShort(0.toShort())
            writer.close()
        }
    }

    // --- delete ---

    @Test
    fun testDeleteFile() = runTest {
        runOpfsTest { storage ->
            val writer = storage.getWriter("file.bin", append = false)
            writer.writeShort(0.toShort())
            writer.close()

            assertTrue(storage.exists("file.bin"))
            storage.delete("file.bin", recursive = false)
            assertFalse(storage.exists("file.bin"))
        }
    }

    @Test
    fun testDeleteDirectoryRecursively() = runTest {
        runOpfsTest { storage ->
            storage.createDirectories("tree/a")
            val writer = storage.getWriter("tree/a/b.bin", append = false)
            writer.writeShort(0.toShort())
            writer.close()

            assertTrue(storage.exists("tree"))
            storage.delete("tree", recursive = true)
            assertFalse(storage.exists("tree"))
        }
    }
}
