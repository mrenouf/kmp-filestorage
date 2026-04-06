package com.bitgrind.filestorage

import kotlin.coroutines.CoroutineContext

interface FileStorage {
    suspend fun readText(path: String): String
    suspend fun writeText(path: String, content: String, append: Boolean)
    suspend fun getReader(path: String): ByteReader
    suspend fun getWriter(path: String,  append: Boolean): ByteWriter
    suspend fun createDirectories(path: String)
    suspend fun delete(path: String, recursive: Boolean)
    suspend fun exists(path: String): Boolean
}

expect fun getFileStorage(context: CoroutineContext): FileStorage