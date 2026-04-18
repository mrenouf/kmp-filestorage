package com.bitgrind.filestorage

interface ByteWriter {
    suspend fun writeByte(byte: Byte)
    suspend fun writeShort(short: Short)
    suspend fun writeInt(int: Int)
    suspend fun writeLong(long: Long)
    suspend fun writeVarint(long: Long) {
        require(long >= 0) { "Varint must be non-negative" }
        var v = long
        while (v and 0x7fL.inv() != 0L) {
            writeByte(((v and 0x7fL) or 0x80L).toByte())
            v = v ushr 7
        }
        writeByte((v and 0x7fL).toByte())
    }
    suspend fun writeString(string: String)
    suspend fun writeByteArray(array: ByteArray)
    suspend fun writeByteArray(array: ByteArray, offset: Int, length: Int)
    suspend fun close()

    suspend fun writeLine(string: String) {
        writeString(string + "\n")
    }
}
