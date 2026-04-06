package com.bitgrind.filestorage.test

import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.impl.OpfsFileStorage
import com.bitgrind.filestorage.impl.fileSystemRemoveOptions
import js.iterable.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import web.fs.removeEntry
import web.navigator.navigator
import web.storage.getDirectory
import kotlin.js.ExperimentalWasmJsInterop

/**
 * Obtains the OPFS root, runs [block] with a scoped [OpfsFileStorage], then
 * recursively removes all entries created during the test.
 * Skips silently if OPFS is not available in the current environment.
 */
@ExperimentalWasmJsInterop
actual suspend fun runFileStorageTest(block: suspend (FileStorage) -> Unit) {
    val storageManager = try {
        navigator.storage
    } catch (_: Exception) {
        println("OPFS not available — skipping")
        return
    }
    val opfsRoot = storageManager.getDirectory()

    val storage: FileStorage = OpfsFileStorage(
        storage = storageManager,
        scope = CoroutineScope(currentCoroutineContext())
    )

    try {
        block(storage)
    } finally {
        opfsRoot.values().asFlow().collect { entry ->
            opfsRoot.removeEntry(entry.name, fileSystemRemoveOptions(recursive = true))
        }
    }
}
