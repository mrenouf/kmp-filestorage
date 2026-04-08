package com.bitgrind.filestorage.testing

import kotlinx.io.Buffer
import kotlinx.io.RawSource

/**
 * A [kotlinx.io.RawSource] that delivers exactly one chunk per [readAtMostTo] call, regardless of `byteCount`.
 * This forces the [kotlinx.io.Source] wrapper to call back for each chunk, exercising the same
 * cross-boundary read paths that a real network or file source would trigger.
 */
internal class ChunkedRawSource(chunks: List<ByteArray>) : RawSource {
    private val queue = ArrayDeque(chunks)

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
        val chunk = queue.removeFirstOrNull()?.let {
            if (it.size <= byteCount) {
                it
            } else {
                queue.addFirst(it.copyOfRange(byteCount.toInt(), it.size))
                it.copyOfRange(0, byteCount.toInt())
            }
        } ?: return -1
        sink.write(chunk)
        return chunk.size.toLong()
    }

    override fun close() {}
}