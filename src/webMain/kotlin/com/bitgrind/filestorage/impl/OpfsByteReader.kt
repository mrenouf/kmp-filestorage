@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import js.buffer.ArrayBuffer
import js.iterable.asFlow
import js.numbers.JsNumbers.toKotlinUByte
import js.numbers.JsUByte
import js.typedarrays.Uint8Array
import js.typedarrays.toUByteArray
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.io.IOException
import web.file.File
import web.streams.ReadableStream
import web.streams.cancel
import kotlin.js.ExperimentalWasmJsInterop

/**
 * A pull-based async byte reader backed by a Js [File].
 *
 * Reads are satisfied from an internal buffer refilled on demand from the flow.
 * Reads that span chunk boundaries are handled transparently by coalescing
 * buffered tail bytes with incoming chunks until enough data is available.
 *
 * The [scope] is used to launch the coroutine that collects the flow into an
 * internal channel. Call [close] to cancel that coroutine when done reading early.
 */
internal class OpfsByteReader internal /* ForTesting */ constructor(
    flow: Flow<Chunk>,
    scope: CoroutineScope,
    onClose: (suspend () -> Unit)? = null
) : ByteReader {

    constructor(
        stream: ReadableStream<Chunk>,
        scope: CoroutineScope,
    ) : this(
        flow = stream.asFlow(),
        scope = scope,
        onClose = {  stream.cancel() }
    )

    companion object {
        fun of(file: File, scope: CoroutineScope) {
            val stream = file.stream()
            OpfsByteReader(stream.asFlow(), scope)
        }
    }

    // TODO: Can this be made any more efficient?
    fun toInt(x: JsUByte): Int = x.toKotlinUByte().toInt()

    private val channel: Channel<Chunk> = Channel<Chunk>(Channel.BUFFERED)

    init {
        scope.launch {
            try {
                flow.collect {
                    channel.send(it)
                }
            } catch (_: ClosedSendChannelException) {
                /* Ignore */
            } finally {
                onClose?.invoke()
                channel.close()
            }
        }
    }

    private var current: Chunk = Uint8Array(0)
    private var position: Int = 0

    private val available: Int get() = current.length - position

    /**
     * Ensures at least [needed] bytes are buffered in [current] starting at [position].
     * Pulls additional chunks from the channel if necessary, coalescing across boundaries.
     * Throws [IllegalStateException] if the stream ends before [needed] bytes are available.
     */
    private suspend fun ensureAvailable(needed: Int) {
        if (available >= needed) return

        val parts = mutableListOf<Chunk>()
        var accumulated = 0

        // Retain unread tail of the current buffer
        if (position < current.length) {
            val tail = current.slice(position, current.length)
            parts.add(tail)
            accumulated = tail.length
        }

        while (accumulated < needed) {
            val result = channel.receiveCatching()
            if (result.isFailure) {
                throw IOException(
                    "Stream ended unexpectedly: need $needed bytes but only $accumulated available"
                )
            }
            val chunk = result.getOrThrow()
            parts.add(chunk)
            accumulated += chunk.length
        }

        // Combine all parts into a single backing buffer
        current = Uint8Array<ArrayBuffer>(accumulated).also { combined ->
            var offset = 0
            for (part in parts) {
                combined.set(part, offset)
                offset += part.length
            }
        }
        position = 0
    }

    override suspend fun readByte(): Byte {
        ensureAvailable(1)
        val byte = current[position]
        position++
        return byte.toKotlinUByte().toByte()
    }


    /** Reads a big-endian signed 16-bit integer. */
    override suspend fun readShort(): Short {
        ensureAvailable(2)
        val value = (toInt(current[position]) shl 8) or
                (toInt(current[position + 1]) and 0xFF)
        position += 2
        return value.toShort()
    }

    /** Reads a big-endian signed 32-bit integer. */
    override suspend fun readInt(): Int {
        ensureAvailable(4)
        val value = ((toInt(current[position]) and 0xFF) shl 24) or
                ((toInt(current[position + 1]) and 0xFF) shl 16) or
                ((toInt(current[position + 2]) and 0xFF) shl 8) or
                (toInt(current[position + 3]) and 0xFF)
        position += 4
        return value
    }

    /**
     * Reads a UTF-8 string prefixed by an unsigned 16-bit byte length.
     * Wire format: [short: byteLength][byteLength bytes: UTF-8 encoded string]
     */
    override suspend fun readString(): String {
        val length = readShort().toInt() and 0xFFFF
        ensureAvailable(length)
        val bytes = current.slice(position, position + length)
        position += length
        return Utf8.decode(bytes)
    }

    /**
     * Reads a byte array prefixed by an unsigned 16-bit length.
     * Wire format: [short: length][length bytes]
     */
    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun readByteArray(): ByteArray {
        val length = readShort().toInt() and 0xFFFF
        ensureAvailable(length)
        val bytes = current.slice(position, position + length)
        position += length
        return bytes.toUByteArray().toByteArray()
    }

    /**
     * Cancels the underlying collector coroutine and closes the channel.
     * Should be called when finished reading before the stream is exhausted.
     */
    override suspend fun close() {
        channel.cancel()
    }
}