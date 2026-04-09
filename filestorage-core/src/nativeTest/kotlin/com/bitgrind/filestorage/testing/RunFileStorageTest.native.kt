package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.random.Random

/**
 * Create a MultiPlatformFileStorage scoped to a root path within
 * a temp dir, runs [block], then recursively removes the temp dir.
 */
actual suspend fun runFileStorageTest(block: suspend (FileStorage, path: String) -> Unit) {
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

    val storage: FileStorage = MultiPlatformFileStorage()

    fun createTempDirectory(name: String): Path {
        val tempDir = Path(storage.systemTempDir, name + Random.nextLong().toULong().toString(16) )
        if (fs.exists(tempDir)) {
            tempDir.deleteRecursively()
        }
        fs.createDirectories(tempDir, mustCreate = true)
        return tempDir
    }

    val tempDir = createTempDirectory("filestorage-test-")
    try {
        block(storage, tempDir.toString())
    } finally {
        tempDir.deleteRecursively()
    }
}
