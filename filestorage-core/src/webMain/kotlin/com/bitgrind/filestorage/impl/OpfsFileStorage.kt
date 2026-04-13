@file:OptIn(ExperimentalWasmJsInterop::class)
package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.ByteWriter
import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.internal.FileStorageTestApi
import com.bitgrind.filestorage.tempName
import js.iterable.asFlow
import js.typedarrays.toUint8Array
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.io.IOException
import kotlinx.io.files.FileNotFoundException
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
) : FileStorage, FileStorageTestApi {
    companion object {
        const val BUFFER_SIZE = 4096
        const val MAX_BUFFER_SIZE = Int.MAX_VALUE
        const val CR = '\r'.code.toByte()
        const val LF = '\n'.code.toByte()
    }
    override val systemTempDir: String = "/tmp"
    override val systemPathSeparator: String = "/"

    private val textEncoder = TextEncoder()

    override var random: Random = Random

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
        val fileHandle = requireFile(path, createIfMissing = false).fileHandle
        return fileHandle.getFile().text()
    }

    override suspend fun readBytes(path: String): ByteArray {
        val fileHandle = requireFile(path, createIfMissing = false).fileHandle
        return fileHandle.getFile().byteArray()
    }

    override suspend fun writeText(path: String, content: String, append: Boolean) {
        val fileHandle = requireFile(path, createIfMissing = true).fileHandle
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
        val fileHandle = requireFile(path, createIfMissing = true).fileHandle
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
        val fileHandle = requireFile(path, createIfMissing = false).fileHandle
        val stream = fileHandle.getFile().stream()
        return OpfsByteReader(stream.getReader())
    }

    override suspend fun getWriter(path: String, append: Boolean): ByteWriter {
        val fileHandle = requireFile(path, createIfMissing = true).fileHandle
        val stream = fileHandle.createWritable(fileSystemCreateWritableOptions(append))
        if (append) {
            stream.seek(fileHandle.getFile().size)
        }
        return OpfsByteWriter(stream)
    }

    override suspend fun createDirectories(path: String) {
        requireDirectory(path, createIfMissing = true)
    }

    override suspend fun delete(path: String, recursive: Boolean) {
        when (val resolved = resolve(getRoot(), path)) {
            is FileStorageEntry.Directory -> resolved.parentHandle.removeEntry(
                resolved.name, fileSystemRemoveOptions(recursive = recursive)
            )
            is FileStorageEntry.File -> resolved.parentHandle.removeEntry(resolved.name)
            is FileStorageEntry.InvalidPath -> throw IOException("Invalid path: $path")
            is FileStorageEntry.NotFound -> throw IOException("File not found: $path")
        }
    }

    override suspend fun exists(path: String): Boolean {
        return when (resolve(getRoot(), path)) {
            is FileStorageEntry.File,
            is FileStorageEntry.Directory -> true
            else -> false
        }
    }

    override suspend fun createTempFile(path: String?, prefix: String?, suffix: String?): String {
        val file = tempName(path, prefix, suffix, random = random)
        requireFile(file, createDirs = true, createIfMissing = true, failIfExists = true)
        return file
    }

    override suspend fun createTempDir(path: String?, prefix: String?): String {
        val dir = tempName(path, prefix, random = random)
        requireDirectory(dir, createIfMissing = true, failIfExists = true)
        return dir
    }

    @OptIn(OpfsUndocumented::class)
    override suspend fun move(source: String, destination: String) {
        val root = getRoot()
        val source = resolve(root, source)
        when (source) {
            is FileStorageEntry.NotFound -> throw IOException("move: source not found: $source")
            is FileStorageEntry.InvalidPath -> throw IOException("move: invalid source path: $source")
            else -> {}
        }
        val dest = resolve(root, destination, createDirs = true)
        if (dest is FileStorageEntry.InvalidPath) {
            throw IOException("move: invalid destination path: $destination")
        }
        when (source) {
            is FileStorageEntry.NotFound -> throw IOException("move: source not found: $source")
            is FileStorageEntry.File -> {
                when (dest) {
                    is FileStorageEntry.Directory -> {
                        source.fileHandle.move(dest.directoryHandle, source.name)
                    }
                    is FileStorageEntry.File -> source.fileHandle.move(dest.parentHandle, dest.name)
                    is FileStorageEntry.NotFound -> source.fileHandle.move(dest.parentHandle, dest.name)
                }
            }
            is FileStorageEntry.Directory -> {
                if (dest !is FileStorageEntry.NotFound) {
                    throw IOException("move: destination already exists: $destination")
                }
                moveDirectoryTo(source.parentHandle, source.directoryHandle, dest.parentHandle, dest.name)
            }
        }
    }

    /**
     * Recursively moves all contents of [srcHandle] into a new directory named [destName] under
     * [destParent], then removes the now-empty [srcHandle] from [srcParent].
     */
    @OptIn(OpfsUndocumented::class)
    private suspend fun moveDirectoryTo(
        srcParent: FileSystemDirectoryHandle,
        srcHandle: FileSystemDirectoryHandle,
        destParent: FileSystemDirectoryHandle,
        destName: String,
    ) {
        val dest = destParent.getDirectoryHandle(destName, fileSystemGetDirectoryOptions(create = true))
        // Snapshot entries before moving to avoid modification-during-iteration issues.
        val entries = srcHandle.values().asFlow().toList()
        for (entry in entries) {
            when (entry.kind) {
                FileSystemHandleKind.file ->
                    (entry as FileSystemFileHandle).move(dest, entry.name)
                FileSystemHandleKind.directory ->
                    moveDirectoryTo(srcHandle, entry as FileSystemDirectoryHandle, dest, entry.name)
            }
        }
        srcParent.removeEntry(srcHandle.name)
    }

    internal suspend fun resolve(
        root: FileSystemDirectoryHandle,
        path: String,
        createDirs: Boolean = false,
    ): FileStorageEntry {
        require(path.isNotEmpty()) { "path must not be empty" }

        var parent: FileSystemDirectoryHandle = root
        val segments = path.segments()
        for ((index, name) in segments.withIndex()) {
            val entry: FileSystemHandle? = parent.values().asFlow().firstOrNull { it.name == name }
            when (entry?.kind) {
                FileSystemHandleKind.file -> {
                    return if (index < segments.lastIndex) FileStorageEntry.InvalidPath(parent, name)
                    else FileStorageEntry.File(parent, name, entry as FileSystemFileHandle)
                }
                FileSystemHandleKind.directory -> {
                    if (index < segments.lastIndex) parent = entry as FileSystemDirectoryHandle
                    else return FileStorageEntry.Directory(parent, name, entry as FileSystemDirectoryHandle)
                }
                null -> {
                    if (index < segments.lastIndex && createDirs) {
                        parent = parent.getDirectoryHandle(name, fileSystemGetDirectoryOptions(create = true))
                    } else {
                        return FileStorageEntry.NotFound(parent, name)
                    }
                }
            }
        }
        return FileStorageEntry.NotFound(parent, "")
    }

    internal suspend fun requireDirectory(
        path: String,
        createIfMissing: Boolean = false,
        failIfExists: Boolean = false,
    ): FileStorageEntry.Directory {
        return when (val resolved = resolve(getRoot(), path, createDirs = createIfMissing)) {
            is FileStorageEntry.File -> throw IOException("Path is a file: $path")

            is FileStorageEntry.Directory ->
                if (failIfExists) {
                    throw IOException("Directory $path already exists")
                } else {
                    resolved
                }

            is FileStorageEntry.InvalidPath ->
                throw IOException("Path is invalid: $path")

            is FileStorageEntry.NotFound -> if (createIfMissing) {
                val dir = resolved.parentHandle.getDirectoryHandle(
                    resolved.name, fileSystemGetDirectoryOptions(create = true)
                )
                FileStorageEntry.Directory(resolved.parentHandle, resolved.name, dir)
            } else {
                throw FileNotFoundException(path)
            }
        }
    }

    internal suspend fun requireFile(
        path: String,
        createDirs: Boolean = false,
        createIfMissing: Boolean = false,
        failIfExists: Boolean = false,
    ): FileStorageEntry.File {
        return when (val resolved = resolve(getRoot(), path, createDirs = createDirs)) {
            is FileStorageEntry.File -> if (failIfExists) {
                throw IOException("File $path already exists")
            } else {
                resolved
            }

            is FileStorageEntry.Directory ->
                throw IOException("Path is a directory: $path")

            is FileStorageEntry.InvalidPath ->
                throw IOException("Path is invalid: $path")

            is FileStorageEntry.NotFound -> if (createIfMissing) {
                val file = resolved.parentHandle.getFileHandle(
                    resolved.name, fileSystemGetFileOptions(create = true)
                )
                FileStorageEntry.File(resolved.parentHandle, resolved.name, file)
            } else {
                throw FileNotFoundException(path)
            }
        }
    }
}


