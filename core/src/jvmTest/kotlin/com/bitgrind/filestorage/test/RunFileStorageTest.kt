package com.bitgrind.filestorage.test

import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.io.files.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolute
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest

/**
 * Create a MultiPlatformFileStorage scoped to a root path within
 * a temp dir, runs [block], then recursively removes the temp dir.
 */
@ExperimentalPathApi
actual suspend fun runFileStorageTest(block: suspend (FileStorage) -> Unit) {

    val tempDir = createTempDirectory("opfs-test")

    val storage: FileStorage = MultiPlatformFileStorage(
        scope = CoroutineScope(currentCoroutineContext()),
        root = Path(tempDir.absolute().toString())
    )

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    try {
        block(storage)
    } finally {
        tempDir.deleteRecursively()
    }
}
