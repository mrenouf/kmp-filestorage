package com.bitgrind.filestorage

/**
 * Wire format conventions:
 * - Integer and Short are big-endian
 * - Strings and byte arrays are prefixed by an unsigned 16-bit length
 */
interface ByteReader {
    suspend fun readShort(): Short
    suspend fun readInt(): Int
    suspend fun readString(): String
    suspend fun readByteArray(): ByteArray
    suspend fun close()
}