package com.bitgrind.filestorage

import kotlinx.coroutines.flow.flow

interface ByteReader {
    suspend fun exhausted(): Boolean

    suspend fun readByte(): Byte
    suspend fun readShort(): Short
    suspend fun readInt(): Int
    suspend fun readLong(): Long
    suspend fun readVarint(): Long {
        var result = 0L
        var shift = 0
        while(true) {
            val byte = readByte().toLong()
            result = result or ((byte and 0x7fL) shl shift)
            if (byte and 0x80L == 0L) break
            shift += 7
            if (shift > 63) {
                throw IllegalArgumentException("Invalid varint value: overflow while decoding")
            }
        }
        return result
    }
    suspend fun readString(length: Int): String
    suspend fun readLine(): String?
    suspend fun readByteArray(length: Int): ByteArray
    suspend fun close()

    fun readLines() = flow {
        while (true) {
            val line = readLine()
            if (line != null) emit(line) else break
        }
    }


}