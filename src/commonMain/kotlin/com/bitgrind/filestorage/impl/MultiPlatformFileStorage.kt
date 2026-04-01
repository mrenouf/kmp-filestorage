package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.api.ByteReader
import com.bitgrind.filestorage.api.ByteWriter
import com.bitgrind.filestorage.api.FileStorage
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * Handles all platforms except for Js and WasmJs browser targets.
 *
 * Uses kotlinx.io [SystemFileSystem]
 *
 * For Js and WasmJS browser, use `OpfsStorage`.
 */
class MultiPlatformFileStorage : FileStorage {

    override suspend fun createDirectories(path: String) {
        SystemFileSystem.createDirectories(Path(path))
    }

    override suspend fun getReader(path: String): ByteReader {
        val source = SystemFileSystem.source(Path(path)).buffered()
        return SourceReader(source)
    }

    override suspend fun getWriter(path: String, append: Boolean): ByteWriter {
        val sink = SystemFileSystem.sink(Path(path), append).buffered()
        return SinkWriter(sink)
    }

    override suspend fun delete(path: String, recursive: Boolean) = delete(Path(path), recursive)

    override suspend fun exists(path: String): Boolean = SystemFileSystem.exists(Path(path))

    private fun delete(path: Path, recursive: Boolean) {
        val isDir = SystemFileSystem.metadataOrNull(path)?.isDirectory ?: false
        if (isDir && recursive) {
            SystemFileSystem.list(path).forEach { child ->
                delete(child, recursive)
            }
        }
        SystemFileSystem.delete(path)
    }
}