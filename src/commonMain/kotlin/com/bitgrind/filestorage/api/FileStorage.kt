package com.bitgrind.filestorage.api

interface FileStorage {
    suspend fun getReader(path: String): ByteReader
    suspend fun getWriter(path: String,  append: Boolean): ByteWriter
    suspend fun createDirectories(path: String)
    suspend fun delete(path: String, recursive: Boolean)
    suspend fun exists(path: String): Boolean
}

expect fun getFileStorage(): FileStorage