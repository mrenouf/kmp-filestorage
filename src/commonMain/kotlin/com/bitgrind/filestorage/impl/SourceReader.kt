package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readString

internal class SourceReader(private val source: Source) : ByteReader {
    override suspend fun readShort(): Short {
        return source.readShort()
    }

    override suspend fun readInt(): Int {
        return source.readInt()
    }

    override suspend fun readString(): String {
        val length = source.readShort().toInt() and 0xFFFF
        return source.readString(length.toLong())
    }

    override suspend fun readByteArray(): ByteArray {
        val length = source.readShort().toInt() and 0xFFFF
        return source.readByteArray(length)
    }

    override suspend fun close() {
        source.close()
    }
}