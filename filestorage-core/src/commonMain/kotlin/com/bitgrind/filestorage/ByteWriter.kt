package com.bitgrind.filestorage

interface ByteWriter {
    suspend fun writeByte(byte: Byte)
    suspend fun writeShort(short: Short)
    suspend fun writeInt(int: Int)
    suspend fun writeLong(long: Long)
    suspend fun writeString(string: String)
    suspend fun writeByteArray(array: ByteArray)
    suspend fun writeByteArray(array: ByteArray, offset: Int, length: Int)
    suspend fun close()
}