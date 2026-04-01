package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString

/**
 * Handles all platforms except for Js and WasmJs browser targets.
 *
 * Uses kotlinx.io [SystemFileSystem]
 *
 * For Js and WasmJS browser, use `OpfsStorage`.
 */
@Suppress("unused")
class MultiPlatformFileStorage(
    private val scope: CoroutineScope,
    /** Intended for testing. */
    private val root: Path = Path("/")
) : FileStorage {

    override suspend fun createDirectories(path: String) {
        SystemFileSystem.createDirectories(Path(root, path))
    }

    override suspend fun readText(path: String): String {
        val source = SystemFileSystem.source(Path(root, path)).buffered()
        return source.readString()
    }

    override suspend fun writeText(path: String, content: String, append: Boolean) {
        val sink = SystemFileSystem.sink(Path(root, path), append).buffered()
        sink.writeString(content)
        sink.flush()
    }

    override suspend fun getReader(path: String): ByteReader {
        val source = SystemFileSystem.source(Path(root, path)).buffered()
        return SourceReader(source)
    }

    override suspend fun getWriter(path: String, append: Boolean): ByteWriter {
        val sink = SystemFileSystem.sink(Path(root, path), append).buffered()
        return SinkWriter(sink)
    }

    override suspend fun delete(path: String, recursive: Boolean) = delete(Path(root, path), recursive)

    override suspend fun exists(path: String): Boolean = SystemFileSystem.exists(Path(root, path))

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