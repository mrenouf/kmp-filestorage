@file:OptIn(ExperimentalWasmJsInterop::class)
package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.tempName
import js.iterable.asFlow
import js.typedarrays.toUint8Array
import kotlinx.coroutines.flow.any
import kotlinx.io.IOException
import web.blob.byteArray
import web.blob.text
import web.encoding.TextEncoder
import web.fs.*
import web.storage.StorageManager
import web.storage.getDirectory
import web.streams.close
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.random.Random

class OpfsFileStorage(
    private val storage: StorageManager,
) : FileStorage {
    companion object {
        const val BUFFER_SIZE = 4096
        const val MAX_BUFFER_SIZE = 1_048_576
    }
    override val systemTempDir: String = "/tmp"
    override val systemPathSeparator: String = "/"

    init {
        println("[OpfsFileStorage]")
        println("systemTempDir: $systemTempDir")
        println("systemPathSeparator: $systemPathSeparator")

     }

    private val textEncoder = TextEncoder()

    internal var random: Random = Random

    internal suspend fun getRoot(): FileSystemDirectoryHandle {
        return storage.getDirectory().also { root ->
            root.mkdirs(systemTempDir)
        }
    }

    internal suspend fun FileSystemDirectoryHandle.mkdirs(path: String): FileSystemDirectoryHandle {
        val segments = path.segments()
        var dir = this
        for (segment in segments) {
            dir = dir.getDirectoryHandle(segment, fileSystemGetDirectoryOptions(create = true))
        }
        return dir
    }

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

    override suspend fun readBytes(path: String): ByteArray {
        val fileHandle = getFileHandle(path, createFile = false)
        return fileHandle.getFile().byteArray()
    }

    override suspend fun writeText(path: String, content: String, append: Boolean) {
        val fileHandle = getFileHandle(path, createFile = true)
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        stream.write(textEncoder.encode(content))
        stream.close()
    }

    override suspend fun writeBytes(
        path: String,
        content: ByteArray,
        startIndex: Int,
        endIndex: Int,
        append: Boolean
    ) {
        val fileHandle = getFileHandle(path, createFile = true)
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        val writeArray = if (startIndex == 0 && endIndex == content.size) {
            content
        } else {
            content.copyOfRange(startIndex, endIndex)
        }
        stream.write(writeArray.toUint8Array())
        stream.close()
    }

    override suspend fun getReader(path: String): ByteReader {
        val fileHandle = getFileHandle(path, createFile = false)
        val stream = fileHandle.getFile().stream()
        return OpfsByteReader(stream.getReader())
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
            println("createDirectories: $segment")
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
        println("exists: $path\n")
        var dir = getRoot()
        val segments = path.segments()
        if (segments.isEmpty()) return false
        if (segments.size == 1) {
            return dir.values().asFlow().any { it.name == segments.target() }
        }
        for (segment in segments.parent()) {
            println("exists: ${dir}.getDirectoryHandle($segment)\n")
            dir = dir.getDirectoryHandle(segment)
            println("exists: $dir\n")

        }
        return dir.values().asFlow().any { it.name == segments.target() }
    }

    override suspend fun createTempFile(path: String?, prefix: String?, suffix: String?): String {
        val file = tempName(path, prefix, suffix, random = random)
        println("createTempFile: $file\n")
        if (exists(file)) { throw IOException("File $file already exists") }
        getFileHandle(file, createFile = true)
        return file
    }

    override suspend fun createTempDir(path: String?, prefix: String?): String {
        val dir = tempName(path, prefix, random = random)
        println("createTempDir: $dir")
        if (exists(dir)) { throw IOException("Directory $dir already exists") }
        createDirectories(dir)
        return dir
    }
}


