package com.bitgrind.filestorage.impl

import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemFileHandle

sealed interface FileStorageEntry {
    val parentHandle: FileSystemDirectoryHandle
    val name: String

    data class File(
        override val parentHandle: FileSystemDirectoryHandle,
        override val name: String,
        val fileHandle: FileSystemFileHandle,
    ) : FileStorageEntry

    data class Directory(
        override val parentHandle: FileSystemDirectoryHandle,
        override val name: String,
        val directoryHandle: FileSystemDirectoryHandle,
    ) : FileStorageEntry

    data class NotFound(
        override val parentHandle: FileSystemDirectoryHandle,
        override val name: String,
    ) : FileStorageEntry

    data class InvalidPath(
        override val parentHandle: FileSystemDirectoryHandle,
        override val name: String,
    ) : FileStorageEntry
}