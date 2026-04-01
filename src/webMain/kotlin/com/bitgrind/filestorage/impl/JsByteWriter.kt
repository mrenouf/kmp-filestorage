@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.api.ByteWriter
import js.buffer.ArrayBuffer
import js.numbers.JsNumbers.toJsUByte
import js.typedarrays.Uint8Array
import js.typedarrays.toUint8Array
import web.fs.FileSystemWritableFileStream
import web.fs.write
import web.streams.close
import kotlin.js.ExperimentalWasmJsInterop

internal class JsByteWriter(private val stream: FileSystemWritableFileStream) : ByteWriter {

    private typealias Chunk = Uint8Array<ArrayBuffer>

    private val buffer: Chunk = Uint8Array(4096)
    private var position: Int = 0

    private val remaining: Int get() = buffer.length - position

    private suspend fun flush() {
        if (position > 0) {
            stream.write(buffer.slice(0, position))
            position = 0
        }
    }

    /**
     * Ensures at least [needed] bytes are available in [buffer] at [position].
     * Flushes the buffer to the stream if there is insufficient remaining space.
     */
    private suspend fun ensureRemaining(needed: Int) {
        if (remaining < needed) {
            flush()
        }
    }

    /** Writes a big-endian signed 16-bit integer. */
    override suspend fun writeShort(short: Short) {
        ensureRemaining(2)
        val value = short.toInt()
        buffer[position++] = ((value ushr 8) and 0xFF).toUByte().toJsUByte()
        buffer[position++] = (value and 0xFF).toUByte().toJsUByte()
    }

    /** Writes a big-endian signed 32-bit integer. */
    override suspend fun writeInt(int: Int) {
        ensureRemaining(4)
        buffer[position++] = ((int ushr 24) and 0xFF).toUByte().toJsUByte()
        buffer[position++] = ((int ushr 16) and 0xFF).toUByte().toJsUByte()
        buffer[position++] = ((int ushr 8) and 0xFF).toUByte().toJsUByte()
        buffer[position++] = (int and 0xFF).toUByte().toJsUByte()
    }

    /**
     * Writes a byte array prefixed by an unsigned 16-bit length.
     * Wire format: [short: length][length bytes]
     * Arrays larger than the internal buffer are written in chunks.
     */
    override suspend fun writeByteArray(array: ByteArray) {
        writeByteArray(array.toUint8Array())
    }

    private suspend fun writeByteArray(array: Uint8Array<ArrayBuffer>) {
        writeShort((array.length and 0xFF).toShort())
        var offset = 0
        while (offset < array.length) {
            ensureRemaining(1)
            val count = minOf(remaining, array.length - offset)
            if (offset + count < array.length) {
                buffer.set(array, offset)
            } else {
                buffer.set(array.slice(offset, offset + count), position)
            }
            position += count
            offset += count
        }
    }

    /**
     * Writes a UTF-8 string prefixed by an unsigned 16-bit byte length.
     * Wire format: [short: byteLength][byteLength bytes: UTF-8 encoded string]
     */
    override suspend fun writeString(string: String) {
        writeByteArray(Utf8.encode(string))
    }

    /**
     * Flushes any remaining buffered bytes and closes the underlying stream.
     */
    override suspend fun close() {
        flush()
        stream.close()
    }
}
