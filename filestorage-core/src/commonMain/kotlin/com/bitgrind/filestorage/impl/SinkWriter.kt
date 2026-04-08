package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteWriter
import kotlinx.io.Sink
import kotlinx.io.writeString

internal class SinkWriter(private val sink: Sink) : ByteWriter {
    override suspend fun writeByte(byte: Byte) {
        sink.writeByte(byte)
    }

    override suspend fun writeShort(short: Short) {
        sink.writeShort(short)
    }

    override suspend fun writeInt(int: Int) {
        sink.writeInt(int)
    }

    override suspend fun writeLong(long: Long) {
        sink.writeLong(long)
    }

    override suspend fun writeString(string: String) {
        sink.writeString(string)
    }

    override suspend fun writeByteArray(array: ByteArray) {
        sink.write(array)
    }

    override suspend fun writeByteArray(array: ByteArray, offset: Int, length: Int) {
        writeByteArray(array.sliceArray(offset until offset + length))
    }

    override suspend fun close() {
        sink.close()
    }
}