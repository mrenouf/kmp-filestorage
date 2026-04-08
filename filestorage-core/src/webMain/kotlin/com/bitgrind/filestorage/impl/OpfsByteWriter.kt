@file:OptIn(ExperimentalWasmJsInterop::class)
package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.impl.OpfsFileStorage.Companion.BUFFER_SIZE
import js.buffer.ArrayBuffer
import js.buffer.DataView
import js.typedarrays.Int8Array
import js.typedarrays.Uint8Array
import js.typedarrays.toInt8Array
import web.encoding.TextEncoder
import web.fs.FileSystemWritableFileStream
import web.fs.write
import web.streams.close
import kotlin.js.ExperimentalWasmJsInterop

internal class OpfsByteWriter(private val stream: FileSystemWritableFileStream) : ByteWriter {

    private val buffer: ArrayBuffer = ArrayBuffer(BUFFER_SIZE)
    private val view = DataView(buffer)
    private val array = Uint8Array(buffer)
    private val int8Array = Int8Array(buffer)
    private val uint8Array = Uint8Array(buffer)
    private var position: Int = 0
    private val remaining: Int get() = array.length - position

    private val textEncoder = TextEncoder()

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

    /** Writes a single signed 8-bit byte. */
    override suspend fun writeByte(byte: Byte) {
        ensureRemaining(1)
        view.setInt8(position++, byte)
    }

    /** Writes a big-endian signed 16-bit integer. */
    override suspend fun writeShort(short: Short) {
        ensureRemaining(2)
        view.setInt16(position, short)
        position += 2
    }

    /** Writes a big-endian signed 32-bit integer. */
    override suspend fun writeInt(int: Int) {
        ensureRemaining(4)
        view.setInt32(position, int)
        position += 4
    }

    override suspend fun writeLong(long: Long) {
        ensureRemaining(8)
        view.setInt32(position, (long shr 32).toInt())
        view.setInt32(position + 4, long.toInt())
        position += 8
    }

    /**
     * Writes a UTF-8 string
     */
    override suspend fun writeString(string: String) {
        writeByteArray(textEncoder.encode(string))
    }

    /**
     * Writes a byte array prefixed by an unsigned 16-bit length.
     * Wire format: [short: length][length bytes]
     * Arrays larger than the internal buffer are written in chunks.
     */
    override suspend fun writeByteArray(array: ByteArray) {
        ensureRemaining(array.size)
        if (remaining >= array.size) {
            int8Array.set(array.toInt8Array(), position)
            position += array.size
        } else {
            flush()
            stream.write(array.toInt8Array())
        }
    }

    override suspend fun writeByteArray(array: ByteArray, offset: Int, length: Int) {
        writeByteArray(array.copyOfRange(offset, offset + length))
    }

    private suspend fun writeByteArray(array: Uint8Array<ArrayBuffer>) {
        ensureRemaining(array.length)
        if (remaining >= array.length) {
            uint8Array.set(array, position)
            position += array.length
        } else {
            flush()
            stream.write(array)
        }
    }

    /**
     * Flushes any remaining buffered bytes and closes the underlying stream.
     */
    override suspend fun close() {
        try {
            flush()
        } finally {
            stream.close()
        }
    }
}
