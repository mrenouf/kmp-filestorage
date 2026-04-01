@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl
import com.bitgrind.filestorage.api.ByteReader
import com.bitgrind.filestorage.api.ByteWriter
import com.bitgrind.filestorage.api.FileStorage
import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.any
import web.fs.*
import web.navigator.navigator
import web.storage.StorageManager
import web.storage.getDirectory
import web.streams.cancel
import kotlin.js.ExperimentalWasmJsInterop

class OpfsFileStorage(
    private val storage: StorageManager = navigator.storage,
    private val scope: CoroutineScope = MainScope()
) : FileStorage {
    internal suspend fun getRoot(): FileSystemDirectoryHandle = storage.getDirectory()

    override suspend fun getReader(path: String): ByteReader {
        val segments = path.segments()
        var dir = getRoot()
        for (segment in segments.parent()) {
            dir = dir.getDirectoryHandle(segment)
        }
        val fileHandle = dir.getFileHandle(segments.target(), fileSystemGetFileOptions(create = false))
        val stream = fileHandle.getFile().stream()
        return JsByteReader(stream.asFlow(), scope, onClose = { stream.cancel() })
    }

    override suspend fun getWriter(path: String, append: Boolean): ByteWriter {
        val segments = path.segments()
        var dir = getRoot()
        for (segment in segments.parent()) {
            dir = dir.getDirectoryHandle(segment)
        }
        val fileHandle = dir.getFileHandle(segments.target(), fileSystemGetFileOptions(create = true))
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        return JsByteWriter(stream)
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


