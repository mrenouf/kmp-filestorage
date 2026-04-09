package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.internal.FileStorageTestApi
import com.bitgrind.filestorage.tempName
import kotlinx.io.IOException
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.SystemPathSeparator
import kotlinx.io.files.SystemTemporaryDirectory
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlin.random.Random

/**
 * Handles all platforms except for Js and WasmJs browser targets.
 *
 * Uses kotlinx.io [SystemFileSystem]
 *
 * For Js and WasmJS browser, use `OpfsStorage`.
 */
@Suppress("unused")
class MultiPlatformFileStorage : FileStorage, FileStorageTestApi {

    override val systemTempDir: String = SystemTemporaryDirectory.toString().ifEmpty { "/tmp" }
    override val systemPathSeparator: String = SystemPathSeparator.toString()

    override var random: Random = Random

    init {
        println("[MultiPlatformFileStorage]")
        println("systemTempDir: $systemTempDir")
        println("systemPathSeparator: $systemPathSeparator")
    }

    override suspend fun createDirectories(path: String) {
        SystemFileSystem.createDirectories(Path(path))
    }

    override suspend fun readText(path: String): String {
        val source = SystemFileSystem.source(Path(path)).buffered()
        return source.readString()
    }

    override suspend fun writeText(path: String, content: String, append: Boolean) {
        val sink = SystemFileSystem.sink(Path(path), append).buffered()
        sink.writeString(content)
        sink.close()
    }

    override suspend fun readBytes(path: String): ByteArray {
        val source = SystemFileSystem.source(Path(path)).buffered()
        return source.readByteArray()
    }

    override suspend fun writeBytes(
        path: String,
        content: ByteArray,
        startIndex: Int,
        endIndex: Int,
        append: Boolean
    ) {
        val sink = SystemFileSystem.sink(Path(path), append).buffered()
        sink.write(content, startIndex, endIndex)
        sink.close()
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

    override suspend fun createTempDir(
        path: String?,
        prefix: String?
    ): String {
        val tempDir = Path(tempName(path, prefix, random = random))
        tempDir.parent?.also { parent ->
            if (!SystemFileSystem.exists(parent)) {
                SystemFileSystem.createDirectories(parent, mustCreate = false)
            }
        }
        SystemFileSystem.createDirectories(tempDir, mustCreate = true)
        return tempDir.toString()
    }

    override suspend fun createTempFile(
        path: String?,
        prefix: String?,
        suffix: String?
    ): String {
        val tempFile = Path(tempName(path, prefix, suffix, random))
        if (SystemFileSystem.exists(tempFile)) {
            throw IOException( "File $tempFile already exists")
        }
        tempFile.parent?.also { parent ->
            if (!SystemFileSystem.exists(parent)) {
                SystemFileSystem.createDirectories(parent, mustCreate = false)
            }
        }
        SystemFileSystem.sink(Path(tempFile)).close()
        return tempFile.toString()
    }

    override suspend fun move(source: String, destination: String) {
        SystemFileSystem.atomicMove(Path(source), Path(destination))
    }

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