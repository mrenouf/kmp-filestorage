package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.OpfsFileStorage
import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CreateTempTests {

    @Test
    fun testCreateTempDirExists() = runTest {
        runFileStorageTest { storage, path ->
            val seed = Random.nextInt()
            val expectedPath = storage.tempName(path = path("existing-dir"), random = Random(seed))
            storage.createDirectories(expectedPath)
            (storage as OpfsFileStorage).random = Random(seed)

            assertFailsWith<IOException> {
                storage.createTempDir(path = path("existing-dir"))
            }
        }
    }
    @Test
    fun testCreateTempFileExists() = runTest {
        runFileStorageTest { storage, path ->
            val seed = Random.nextInt()
            val expectedPath = storage.tempName(path = path(storage.systemTempDir), random = Random(seed))
            storage.createDirectories(expectedPath)
            (storage as OpfsFileStorage).random = Random(seed)

            assertFailsWith<IOException> {
                storage.createTempFile(path = path(storage.systemTempDir))
            }
        }
    }
}