package com.bitgrind.filestorage

interface ByteReader {
    suspend fun readByte(): Byte
    suspend fun readShort(): Short
    suspend fun readInt(): Int
    suspend fun readLong(): Long
    suspend fun readString(length: Int): String
    suspend fun readByteArray(length: Int): ByteArray
    suspend fun close()
}