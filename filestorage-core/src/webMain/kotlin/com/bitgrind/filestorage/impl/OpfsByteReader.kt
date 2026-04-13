@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.impl.OpfsFileStorage.Companion.BUFFER_SIZE
import com.bitgrind.filestorage.impl.OpfsFileStorage.Companion.CR
import com.bitgrind.filestorage.impl.OpfsFileStorage.Companion.LF
import com.bitgrind.filestorage.impl.OpfsFileStorage.Companion.MAX_BUFFER_SIZE
import js.buffer.ArrayBuffer
import js.buffer.DataView
import js.typedarrays.Uint8Array
import js.typedarrays.toByteArray
import kotlinx.io.EOFException
import web.encoding.TextDecoder
import web.streams.ReadableStreamDefaultReader
import web.streams.read
import kotlin.js.ExperimentalWasmJsInterop

internal class OpfsByteReader internal /* ForTesting */ constructor(
    private val stream: ReadableStreamDefaultReader<Uint8Array<ArrayBuffer>>,
) : ByteReader {

    private val textDecoder = TextDecoder()

    private val available: Int get() = writePosition - readPosition
    private val remaining: Int get() = buffer.byteLength - writePosition

    private val buffer: ArrayBuffer = ArrayBuffer(BUFFER_SIZE, arrayBufferOptions(MAX_BUFFER_SIZE))
    private val view = DataView(buffer)
    private val uint8Array = Uint8Array(buffer)
    private var readPosition: Int = 0
    private var writePosition: Int = 0

    private suspend fun requireBytes(needed: Int) {
        if (!requestBytes(needed)) {
            throw EOFException("Stream ended unexpectedly: need $needed bytes but only $available available")
        }
    }

    private suspend fun requestBytes(needed: Int): Boolean {
        if (available >= needed) return true
        while (available < needed) {
            val result = stream.read()
            if (result.done) {
                return false
            }
            val read = result.value!!
            val bytesRead = read.byteLength
            if (bytesRead > remaining) {
                var newSize = buffer.byteLength
                while ((newSize - writePosition) < bytesRead) {
                    newSize *= 2
                }
                buffer.resize(newSize)
            }
            uint8Array.set(read, writePosition)
            writePosition += bytesRead
        }
        return true
    }

    override suspend fun exhausted(): Boolean {
        return !requestBytes(1)
    }

    internal fun peekByte(): Byte {
        return view.getInt8(readPosition)
    }

    override suspend fun readByte(): Byte {
        requireBytes(1)
        return view.getInt8(readPosition++)
    }

    /** Reads a big-endian signed 16-bit integer. */
    override suspend fun readShort(): Short {
        requireBytes(2)
        return view.getInt16(readPosition).also { readPosition += 2 }
    }

    /** Reads a big-endian signed 32-bit integer. */
    override suspend fun readInt(): Int {
        requireBytes(4)
        return view.getInt32(readPosition).also { readPosition += 4 }
    }

    override suspend fun readLong(): Long {
        requireBytes(8)
        val i1 = view.getInt32(readPosition)
        val i2 = view.getInt32(readPosition + 4)
        readPosition += 8
        return (i1.toLong() shl 32) or (i2.toLong() and 0xFFFFFFFFL)
    }

    override suspend fun readString(length: Int) = buildString {
        if (length == 0) return@buildString
        var remaining = length
        if (available > 0) {
            val decodeBytes = minOf(available, length)
            val availableBytes = Uint8Array(buffer, readPosition, decodeBytes)
            append(textDecoder.decode(availableBytes, textDecodeOptions(stream = true)))
            readPosition += decodeBytes
            remaining -= decodeBytes
        }
        while (remaining > 0) {
            val result = stream.read()
            if (result.done) {
                if (remaining > 0) {
                    throw EOFException(
                        "Stream ended unexpectedly: need $length bytes but only ${length - remaining} available"
                    )
                }
                break
            }
            val read = result.value!!
            val decodeBytes = minOf(read.length, remaining)
            append(textDecoder.decode(read.slice(0, decodeBytes), textDecodeOptions(stream = true)))
            if (decodeBytes < read.length) {
                uint8Array.set(read.slice(decodeBytes, read.length), writePosition)
                writePosition += (read.length - decodeBytes)
            }
            remaining -= decodeBytes
        }
        append(textDecoder.decode())
    }

    override suspend fun readByteArray(length: Int): ByteArray {
        var remaining = length
        val outBuffer = ArrayBuffer(length)
        val outArray = Uint8Array(outBuffer)
        var outPosition = 0
        if (available > 0) {
            val copyBytes = minOf(available, length)
            val availableBytes = Uint8Array(buffer, readPosition, copyBytes)
            outArray.set(availableBytes, 0)
            readPosition += copyBytes
            outPosition += copyBytes
            remaining -= copyBytes
        }
        while (remaining > 0) {
            val result = stream.read()
            if (result.done) {
                throw EOFException(
                    "Stream ended unexpectedly: need $length bytes but only ${length - remaining} available"
                )
            }
            val read = result.value!!
            val copyBytes = minOf(read.length, remaining)
            if (outPosition + copyBytes > outArray.length) {
                outBuffer.resize(outPosition + copyBytes)
            }
            outArray.set(read.slice(0, copyBytes), outPosition)
            remaining -= copyBytes
            outPosition += copyBytes
            if (copyBytes < read.length) {
                val leftoverLength = read.length - copyBytes
                if (leftoverLength > buffer.byteLength - writePosition) {
                    buffer.resize(writePosition + leftoverLength)
                }
                uint8Array.set(read.slice(copyBytes, read.length), writePosition)
                writePosition += leftoverLength
            }
        }
        return outArray.toByteArray()
    }

    private fun DataView<ArrayBuffer>.endOfLineOrNull(start: Int, end: Int): Int? {
        for (i in start until end) {
            val next = getInt8(i)
            if (next == CR || next == LF) {
                return i
            }
        }
        return null
    }

    override suspend fun readLine(): String? = buildString {
        if (!requestBytes(1)) return null
        while (true) {
            val eolPos = view.endOfLineOrNull(readPosition, writePosition)
            val eolChar = eolPos?.let { view.getInt8(it) }
            val decodeEnd = eolPos ?: writePosition
            val startNext = eolPos?.plus(1) ?: writePosition
            val decodeLength = decodeEnd - readPosition
            append(textDecoder.decode(Uint8Array(buffer, readPosition, decodeLength), textDecodeOptions(stream = true)))
            readPosition = startNext
            if (eolChar == CR && (available > 0 || requestBytes(1)) && peekByte() == LF) {
                readPosition++
            }
            if (eolChar != null || !requestBytes(1)) {
                return toString()
            }
        }
    }

    override suspend fun close() {
    }
}