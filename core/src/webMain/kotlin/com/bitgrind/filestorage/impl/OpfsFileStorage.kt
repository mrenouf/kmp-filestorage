@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl
import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.FileStorage
import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.any
import web.blob.text
import web.fs.*
import web.storage.StorageManager
import web.storage.getDirectory
import web.streams.cancel
import web.streams.close
import kotlin.js.ExperimentalWasmJsInterop

class OpfsFileStorage(
    private val storage: StorageManager,
    private val scope: CoroutineScope,
) : FileStorage {
    internal suspend fun getRoot(): FileSystemDirectoryHandle = storage.getDirectory()

    internal suspend fun getFileHandle(path: String, createFile: Boolean): FileSystemFileHandle {
        val segments = path.segments()
        var dir = getRoot()
        for (segment in segments.parent()) {
            dir = dir.getDirectoryHandle(segment)
        }
        return dir.getFileHandle(segments.target(), fileSystemGetFileOptions(create = createFile))
    }

    override suspend fun readText(path: String): String {
        val fileHandle = getFileHandle(path, createFile = false)
        return fileHandle.getFile().text()
    }

    override suspend fun writeText(path: String, content: String, append: Boolean) {
        val fileHandle = getFileHandle(path, createFile = true)
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        stream.write(Utf8.encode(content))
        stream.close()
    }

    override suspend fun getReader(path: String): ByteReader {
        val fileHandle = getFileHandle(path, createFile = false)
        val stream = fileHandle.getFile().stream()
        return OpfsByteReader(stream.asFlow(), scope, onClose = { stream.cancel() })
    }

    override suspend fun getWriter(path: String, append: Boolean): ByteWriter {
        val fileHandle = getFileHandle(path, createFile = true)
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        return OpfsByteWriter(stream)
    }

    override suspend fun createDirectories(path: String) {
        var dir = getRoot()
        for (segment in path.segments()) {
            dir = dir.getDirectoryHandle(segment, fileSystemGetDirectoryOptions(create = true))
        }
    }

    private suspend fun deleteRecursively(directoryHandle: FileSystemDirectoryHandle) {
        directoryHandle.values().asFlow().collect { entry ->
            if (entry.kind == FileSystemHandleKind.directory) {
                deleteRecursively(entry as FileSystemDirectoryHandle)
            }
            directoryHandle.removeEntry(entry.name)
        }
    }


    override suspend fun delete(path: String, recursive: Boolean) {
        var dir = getRoot()
        val segments = path.segments()
        if (segments.isEmpty()) return
        if (segments.size == 1) {
            dir.removeEntry(segments.first(), fileSystemRemoveOptions(recursive = recursive))
        } else {
            for (segment in segments.parent()) {
                dir = dir.getDirectoryHandle(segment)
            }
            dir.removeEntry(segments.target(), fileSystemRemoveOptions(recursive = recursive))
        }
    }

    override suspend fun exists(path: String): Boolean {
        var dir = getRoot()
        val segments = path.segments()
        if (segments.isEmpty()) return false
        if (segments.size == 1) {
            return dir.values().asFlow().any { it.name == segments.target() }
        }
        for (segment in segments.parent()) {
            dir = dir.getDirectoryHandle(segment)
        }
        return dir.values().asFlow().any { it.name == segments.target() }
    }
}


