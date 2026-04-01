@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalUnsignedTypes::class)

package com.bitgrind.filestorage.impl

import js.typedarrays.toUint8Array
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.test.runTest
import web.file.File
import web.fs.*
import web.navigator.navigator
import web.storage.getDirectory
import web.streams.close
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals


/**
 * Integration tests for [JsByteReader] using a real OPFS file as the byte source.
 *
 * The same 10-byte payload is written once per test and then interpreted three
 * different ways to exercise the reader's framing and cross-buffer logic end-to-end.
 *
 * Byte layout:
 *   offset  0- 1 : 0x00 0x08           → short  0x0008  / high 2 bytes of int 0x00080304
 *   offset  2- 3 : 0x03 0x04           → short  0x0304  / low  2 bytes of int 0x00080304
 *   offset  4- 5 : 0x00 0x04           → unsigned-short length = 4 (string/bytearray prefix)
 *   offset  6- 9 : 0x41 0x42 0x43 0x44 → "ABCD"
 */
class FlowByteReaderOpfsTest {

    private val testBytes = byteArrayOf(
        0x00, 0x08, 0x03, 0x04,        // two shorts / one int
        0x00, 0x04,                     // length prefix = 4
        0x41, 0x42, 0x43, 0x44,        // 'A' 'B' 'C' 'D'
    )

    private val fileName = "flow-byte-reader-test.bin"

    /**
     * Writes [testBytes] to a temporary OPFS file, runs [block] with the root
     * directory handle, then removes the file regardless of outcome.
     * Skips silently if OPFS is not available in the current test environment.
     */
    private suspend fun runOpfsTest(block: suspend (File) -> Unit) {
        val root = try {
            navigator.storage.getDirectory()
        } catch (_: Exception) {
            println("OPFS not available — skipping")
            return
        }

        val file = root.getFileHandle(fileName, fileSystemGetFileOptions(create = true))
        val writable = file.createWritable()
        writable.write(testBytes.toUint8Array())
        writable.close()

        try {
            block(file.getFile())
        } finally {
            root.removeEntry(fileName)
        }
    }

    private suspend fun File.openReader(): JsByteReader {
        return JsByteReader(stream(), CoroutineScope(currentCoroutineContext()))
    }

    // readShort() == 0x0008, readShort() == 0x0304
    @Test
    fun testReadTwoShorts() = runTest {
        runOpfsTest { file ->
            val reader = file.openReader()
            assertEquals(0x0008.toShort(), reader.readShort())
            assertEquals(0x0304.toShort(), reader.readShort())
            reader.close()
        }
    }

    // readInt() == 0x00080304, readString() == "ABCD"
    @Test
    fun testReadIntThenString() = runTest {
        runOpfsTest { file ->
            val reader = file.openReader()
            assertEquals(0x00080304, reader.readInt())
            assertEquals("ABCD", reader.readString())
            reader.close()
        }
    }

    // readByteArray() == [0x03, 0x04, 0x00, 0x04, 0x41, 0x42, 0x43, 0x44]
    @Test
    fun testReadByteArray() = runTest {
        runOpfsTest { file ->
            val reader = file.openReader()
            assertContentEquals(
                byteArrayOf(0x03, 0x04, 0x00, 0x04, 0x41, 0x42, 0x43, 0x44),
                reader.readByteArray()
            )
            reader.close()
        }
    }
}
