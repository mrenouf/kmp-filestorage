package com.bitgrind.filestorage.api

/**
 * Wire format conventions:
 * - Integer and Short are big-endian
 * - Strings and byte arrays are prefixed by an unsigned 16-bit length
 */
interface ByteWriter {
    suspend fun writeShort(short: Short)
    suspend fun writeInt(int: Int)
    suspend fun writeString(string: String)
    suspend fun writeByteArray(array: ByteArray)
    suspend fun close()
}