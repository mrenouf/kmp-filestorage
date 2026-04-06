package com.bitgrind.filestorage.test

import kotlinx.io.files.SystemFileSystem

import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.io.files.Path

/**
 * Create a MultiPlatformFileStorage scoped to a root path within
 * a temp dir, runs [block], then recursively removes the temp dir.
 */
actual suspend fun runFileStorageTest(block: suspend (FileStorage) -> Unit) {
    val fs = SystemFileSystem

    fun Path.deleteRecursively() {
        val meta = fs.metadataOrNull(this) ?: return
        if (meta.isDirectory) {
            fs.list(this).forEach { path ->
                path.deleteRecursively()
            }
        }
        fs.delete(this)
    }

    fun createTempDirectory(name: String): Path {
        val tempDir = Path("/tmp", name)
        if (fs.exists(tempDir)) {
            tempDir.deleteRecursively()
        }
        fs.createDirectories(tempDir, mustCreate = true)
        return tempDir
    }

    val tempDir = createTempDirectory("filestorage-test")

    val storage: FileStorage = MultiPlatformFileStorage(
        scope = CoroutineScope(currentCoroutineContext()),
        root = tempDir
    )

    try {
        block(storage)
    } finally {
        tempDir.deleteRecursively()
    }
}
