package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.absolute
import kotlin.random.Random

expect fun getFileStorage(): FileStorage

interface FileStorage {
    val systemTempDir: String
    val systemPathSeparator: String

    suspend fun readText(path: String): String
    suspend fun writeText(path: String, content: String, append: Boolean)
    suspend fun readBytes(path: String): ByteArray
    suspend fun writeBytes(path: String, content: ByteArray, startIndex: Int = 0, endIndex: Int = content.size, append: Boolean)
    suspend fun getReader(path: String): ByteReader
    suspend fun getWriter(path: String,  append: Boolean): ByteWriter
    suspend fun createDirectories(path: String)
    suspend fun delete(path: String, recursive: Boolean)
    suspend fun exists(path: String): Boolean
    suspend fun createTempDir(path: String? = null, prefix: String? = null): String
    suspend fun createTempFile(path: String? = null, prefix: String? = null, suffix: String? = null): String
}

internal fun FileStorage.tempName(path: String? = null, prefix: String? = null, suffix: String? = null, random: Random = Random): String {
    require(!prefix.orEmpty().contains('/')) { "Invalid prefix: $prefix" }
    require(!suffix.orEmpty().contains('/')) { "Invalid suffix: $suffix" }

    val dir = path?.let { path.absolute(systemPathSeparator) } ?: systemTempDir
    val name = (prefix ?: "tmp-") + random.nextLong().toULong().toString(16) + suffix.orEmpty()
    return "$dir/$name".absolute(systemPathSeparator)
}
