package com.bitgrind.filestorage

import kotlinx.coroutines.flow.flow

interface ByteReader {
    suspend fun exhausted(): Boolean

    suspend fun readByte(): Byte
    suspend fun readShort(): Short
    suspend fun readInt(): Int
    suspend fun readLong(): Long
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