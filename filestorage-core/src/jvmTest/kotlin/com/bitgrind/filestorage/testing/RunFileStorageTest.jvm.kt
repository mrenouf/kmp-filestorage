@file:OptIn(ExperimentalPathApi::class)

package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.FileStorage
import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.io.files.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively

/**
 * Create a MultiPlatformFileStorage scoped to a root path within
 * a temp dir, runs [block], then recursively removes the temp dir.
 */
actual suspend fun runFileStorageTest(block: suspend (FileStorage, path: String) -> Unit) {

    val testRoot = createTempDirectory("filestorage-test")

    val storage: FileStorage = MultiPlatformFileStorage()
    try {
        block(storage, testRoot.absolutePathString())
    } finally {
        testRoot.deleteRecursively()
    }
}
