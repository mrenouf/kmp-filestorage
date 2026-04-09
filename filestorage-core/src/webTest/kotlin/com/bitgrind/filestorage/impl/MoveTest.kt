@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import com.bitgrind.filestorage.testing.runFileStorageTest
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import web.navigator.navigator
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.random.Random
import kotlin.test.*

class MoveTest {

    @Test
    fun moveFileToNewNameInSameDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/source.txt", "hello", append = false)

            storage.move("$basePath/source.txt", "$basePath/dest.txt")

            assertFalse(storage.exists("$basePath/source.txt"))
            assertTrue(storage.exists("$basePath/dest.txt"))
            assertEquals("hello", storage.readText("$basePath/dest.txt"))
        }
    }

    @Test
    fun moveFileToNewDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/source.txt", "hello", append = false)
            // dest parent does not exist yet — createDirs = true creates it

            storage.move("$basePath/source.txt", "$basePath/subdir/dest.txt")

            assertFalse(storage.exists("$basePath/source.txt"))
            assertTrue(storage.exists("$basePath/subdir/dest.txt"))
            assertEquals("hello", storage.readText("$basePath/subdir/dest.txt"))
        }
    }

    @Test
    fun moveFileOverwritesExistingFile() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/source.txt", "new content", append = false)
            storage.writeText("$basePath/dest.txt", "old content", append = false)

            storage.move("$basePath/source.txt", "$basePath/dest.txt")

            assertFalse(storage.exists("$basePath/source.txt"))
            assertEquals("new content", storage.readText("$basePath/dest.txt"))
        }
    }

    @Test
    fun moveFileIntoDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/source.txt", "hello", append = false)
            storage.createDirectories("$basePath/destdir")

            storage.move("$basePath/source.txt", "$basePath/destdir")

            assertFalse(storage.exists("$basePath/source.txt"))
            assertTrue(storage.exists("$basePath/destdir/source.txt"))
            assertEquals("hello", storage.readText("$basePath/destdir/source.txt"))
        }
    }

    // ── Directory source ───────────────────────────────────────────────────────

    @Test
    fun moveDirectoryToNewName() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/srcdir")
            storage.writeText("$basePath/srcdir/file.txt", "content", append = false)

            storage.move("$basePath/srcdir", "$basePath/destdir")

            assertFalse(storage.exists("$basePath/srcdir"))
            assertTrue(storage.exists("$basePath/destdir"))
            assertTrue(storage.exists("$basePath/destdir/file.txt"))
            assertEquals("content", storage.readText("$basePath/destdir/file.txt"))
        }
    }

    @Test
    fun moveDirectoryToNewParentDirectory() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/srcdir")
            storage.writeText("$basePath/srcdir/file.txt", "content", append = false)
            // dest parent does not exist yet — createDirs = true creates it

            storage.move("$basePath/srcdir", "$basePath/parent/destdir")

            assertFalse(storage.exists("$basePath/srcdir"))
            assertTrue(storage.exists("$basePath/parent/destdir"))
            assertTrue(storage.exists("$basePath/parent/destdir/file.txt"))
        }
    }

    @Test
    fun moveDirectoryWithNestedContents() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/srcdir/sub")
            storage.writeText("$basePath/srcdir/a.txt", "a", append = false)
            storage.writeText("$basePath/srcdir/sub/b.txt", "b", append = false)

            storage.move("$basePath/srcdir", "$basePath/destdir")

            assertFalse(storage.exists("$basePath/srcdir"))
            assertTrue(storage.exists("$basePath/destdir/a.txt"))
            assertTrue(storage.exists("$basePath/destdir/sub/b.txt"))
            assertEquals("a", storage.readText("$basePath/destdir/a.txt"))
            assertEquals("b", storage.readText("$basePath/destdir/sub/b.txt"))
        }
    }

    @Test
    fun moveDirectoryToExistingDirectoryThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/srcdir")
            storage.createDirectories("$basePath/destdir")

            assertFailsWith<IOException> {
                storage.move("$basePath/srcdir", "$basePath/destdir")
            }
        }
    }

    @Test
    fun moveDirectoryToExistingFileThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.createDirectories("$basePath/srcdir")
            storage.writeText("$basePath/dest", "x", append = false)

            assertFailsWith<IOException> {
                storage.move("$basePath/srcdir", "$basePath/dest")
            }
        }
    }

    // ── Source errors ──────────────────────────────────────────────────────────

    @Test
    fun moveNonExistentSourceThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            assertFailsWith<IOException> {
                storage.move("$basePath/missing.txt", "$basePath/dest.txt")
            }
        }
    }

    @Test
    fun moveInvalidSourcePathThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/file", "x", append = false)
            // "file" is a file, not a directory — path "file/child" is InvalidPath

            assertFailsWith<IOException> {
                storage.move("$basePath/file/child", "$basePath/dest")
            }
        }
    }

    // ── Destination errors ─────────────────────────────────────────────────────

    @Test
    fun moveToInvalidDestinationPathThrows() = runTest {
        runFileStorageTest { storage, basePath ->
            storage.writeText("$basePath/source.txt", "x", append = false)
            storage.writeText("$basePath/barrier", "x", append = false)
            // "barrier" is a file, so "barrier/dest" is an InvalidPath

            assertFailsWith<IOException> {
                storage.move("$basePath/source.txt", "$basePath/barrier/dest")
            }
        }
    }
}