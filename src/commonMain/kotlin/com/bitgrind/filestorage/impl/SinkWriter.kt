package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteWriter
import kotlinx.io.Sink
import kotlinx.io.writeString

internal class SinkWriter(private val sink: Sink) : ByteWriter {
    override suspend fun writeShort(short: Short) {
        sink.writeShort(short)
    }

    override suspend fun writeInt(int: Int) {
        sink.writeInt(int)
    }

    override suspend fun writeString(string: String) {
        sink.writeShort((string.length and 0xFFFF).toShort())
        sink.writeString(string)
    }

    override suspend fun writeByteArray(array: ByteArray) {
        sink.writeShort((array.size and 0xFFFF).toShort())
        sink.write(array)
    }

    override suspend fun close() {
        sink.close()
    }
}