package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import kotlinx.io.Source
import kotlinx.io.readByteArray
import kotlinx.io.readLine
import kotlinx.io.readString

class SourceReader(private val source: Source) : ByteReader {

    override suspend fun exhausted(): Boolean {
        return source.exhausted()
    }

    override suspend fun readByte(): Byte {
        return source.readByte()
    }

    override suspend fun readShort(): Short {
        return source.readShort()
    }

    override suspend fun readInt(): Int {
        return source.readInt()
    }

    override suspend fun readLong(): Long {
        return source.readLong()
    }

    override suspend fun readString(length: Int): String {
        return source.readString(length.toLong())
    }

    override suspend fun readLine(): String? {
        return source.readLine()
    }

    override suspend fun readByteArray(length: Int): ByteArray {
        return source.readByteArray(length)
    }

    override suspend fun close() {
        source.close()
    }
}