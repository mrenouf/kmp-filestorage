@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import kotlinx.coroutines.test.runTest
import web.fs.FileSystemDirectoryHandle
import web.fs.getDirectoryHandle
import web.fs.getFileHandle
import web.fs.removeEntry
import web.navigator.navigator
import web.storage.getDirectory
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.random.Random
import kotlin.test.*

class PathResolutionTest {

    /**
     * Creates an isolated OPFS directory for a single test, runs [block] with it as the
     * resolve root, then removes the directory unconditionally. Skips silently if OPFS is
     * not available in the current environment.
     */
    private suspend fun runResolveTest(block: suspend OpfsFileStorage.(FileSystemDirectoryHandle) -> Unit) {
        val storage = try {
            OpfsFileStorage(navigator.storage)
        } catch (_: Exception) {
            println("OPFS not available — skipping")
            return
        }
        val root = navigator.storage.getDirectory()
        val testDirName = "resolve-test-${Random.nextInt().toUInt()}"
        val testDir = root.getDirectoryHandle(testDirName, fileSystemGetDirectoryOptions(create = true))
        try {
            storage.block(testDir)
        } finally {
            root.removeEntry(testDirName, fileSystemRemoveOptions(recursive = true))
        }
    }

    // ── Empty / root-only paths ───────────────────────────────────────────────

    @Test
    fun emptyPathFails() = runTest {
        runResolveTest { root ->
            assertFailsWith<IllegalArgumentException> {
                resolve(root, "")
            }
        }
    }

    @Test
    fun slashOnlyPathReturnsNotFound() = runTest {
        runResolveTest { root ->
            val result = resolve(root, "/")
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("", result.name)
        }
    }

    // ── Single-segment paths ──────────────────────────────────────────────────

    @Test
    fun existingFileReturnsFile() = runTest {
        runResolveTest { root ->
            root.getFileHandle("foo.txt", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "foo.txt")
            assertIs<FileStorageEntry.File>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("foo.txt", result.name)
            assertEquals("foo.txt", result.fileHandle!!.name)
        }
    }

    @Test
    fun existingDirectoryReturnsDirectory() = runTest {
        runResolveTest { root ->
            root.getDirectoryHandle("subdir", fileSystemGetDirectoryOptions(create = true))

            val result = resolve(root, "subdir")
            assertIs<FileStorageEntry.Directory>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("subdir", result.name)
            assertEquals("subdir", result.directoryHandle!!.name)
        }
    }

    @Test
    fun missingSegmentReturnsNotFound() = runTest {
        runResolveTest { root ->
            val result = resolve(root, "does-not-exist")
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("does-not-exist", result.name)
        }
    }

    // ── Multi-segment paths ───────────────────────────────────────────────────

    @Test
    fun nestedFileReturnsFile() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            val bDir = aDir.getDirectoryHandle("b", fileSystemGetDirectoryOptions(create = true))
            bDir.getFileHandle("c.txt", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/b/c.txt")
            assertIs<FileStorageEntry.File>(result)
            assertEquals("b", result.parentHandle.name)
            assertEquals("c.txt", result.name)
            assertEquals("c.txt", result.fileHandle!!.name)
        }
    }

    @Test
    fun nestedDirectoryReturnsDirectory() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getDirectoryHandle("b", fileSystemGetDirectoryOptions(create = true))

            val result = resolve(root, "a/b")
            assertIs<FileStorageEntry.Directory>(result)
            assertEquals("a", result.parentHandle.name)
            assertEquals("b", result.name)
            assertEquals("b", result.directoryHandle!!.name)
        }
    }

    @Test
    fun parentHandlePointsToContainingDirectory() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getFileHandle("file.txt", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/file.txt")
            assertIs<FileStorageEntry.File>(result)
            assertEquals("a", result.parentHandle.name)
            assertEquals("file.txt", result.name)
            assertEquals("file.txt", result.fileHandle!!.name)
        }
    }

    // ── Missing intermediate segments ─────────────────────────────────────────

    @Test
    fun missingFirstSegmentReturnsNotFound() = runTest {
        runResolveTest { root ->
            val result = resolve(root, "missing/child")
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("missing", result.name)
        }
    }

    @Test
    fun missingIntermediateSegmentReturnsNotFound() = runTest {
        runResolveTest { root ->
            root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))

            val result = resolve(root, "a/missing/c")
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("missing", result.name)
        }
    }

    // ── InvalidPath: file encountered where a directory is required ───────────

    @Test
    fun fileAsFirstIntermediateSegmentReturnsInvalidPath() = runTest {
        runResolveTest { root ->
            root.getFileHandle("a", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/b")
            assertIs<FileStorageEntry.InvalidPath>(result)
            assertEquals("a", result.name)
        }
    }

    @Test
    fun fileAsMiddleIntermediateSegmentReturnsInvalidPath() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getFileHandle("b", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/b/c")
            assertIs<FileStorageEntry.InvalidPath>(result)
            assertEquals("b", result.name)
        }
    }

    // ── Path normalization (via segments()) ───────────────────────────────────

    @Test
    fun leadingSlashIsNormalized() = runTest {
        runResolveTest { root ->
            root.getFileHandle("foo", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "/foo")
            assertIs<FileStorageEntry.File>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("foo", result.name)
            assertEquals("foo", result.fileHandle!!.name)
        }
    }

    @Test
    fun trailingSlashIsNormalized() = runTest {
        runResolveTest { root ->
            root.getDirectoryHandle("foo", fileSystemGetDirectoryOptions(create = true))

            val result = resolve(root, "foo/")
            assertIs<FileStorageEntry.Directory>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("foo", result.name)
            assertEquals("foo", result.directoryHandle!!.name)
        }
    }

    @Test
    fun dotSegmentIsNormalized() = runTest {
        runResolveTest { root ->
            root.getFileHandle("target", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "./target")
            assertIs<FileStorageEntry.File>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("target", result.name)
            assertEquals("target", result.fileHandle!!.name)
        }
    }

    @Test
    fun dotDotSegmentIsNormalized() = runTest {
        runResolveTest { root ->
            // "a/../target" collapses to "target" — "a" need not even exist
            root.getFileHandle("target", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/../target")
            assertIs<FileStorageEntry.File>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("target", result.name)
            assertEquals("target", result.fileHandle!!.name)
        }
    }

    @Test
    fun complexNormalizationResolvesCorrectly() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getDirectoryHandle("b", fileSystemGetDirectoryOptions(create = true))

            // "a/x/../b" → ["a", "b"]
            val result = resolve(root, "a/x/../b")
            assertIs<FileStorageEntry.Directory>(result)
            assertEquals("a", result.parentHandle.name)
            assertEquals("b", result.name)
            assertEquals("b", result.directoryHandle!!.name)
        }
    }

    // ── createDirs = true ─────────────────────────────────────────────────────

    @Test
    fun createDirsDoesNotCreateTerminalSegment() = runTest {
        runResolveTest { root ->
            val result = resolve(root, "new", createDirs = true)
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("new", result.name)
            // verify no side effect — terminal was not created
            assertIs<FileStorageEntry.NotFound>(resolve(root, "new"))
        }
    }

    @Test
    fun createDirsCreatesMissingIntermediates() = runTest {
        runResolveTest { root ->
            // nothing exists under root
            val result = resolve(root, "a/b/target", createDirs = true)
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("b", result.parentHandle.name)
            assertEquals("target", result.name)
            // "a" and "b" were created as side effects
            val a = resolve(root, "a")
            assertIs<FileStorageEntry.Directory>(a)
            assertEquals(root.name, a.parentHandle.name)
            assertEquals("a", a.directoryHandle!!.name)
            val b = resolve(root, "a/b")
            assertIs<FileStorageEntry.Directory>(b)
            assertEquals("a", b.parentHandle.name)
            assertEquals("b", b.directoryHandle!!.name)
        }
    }

    @Test
    fun createDirsCreatesSingleMissingIntermediate() = runTest {
        runResolveTest { root ->
            root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            // "b" inside "a" does not exist yet
            val result = resolve(root, "a/b/target", createDirs = true)
            assertIs<FileStorageEntry.NotFound>(result)
            assertEquals("b", result.parentHandle.name)
            assertEquals("target", result.name)
            // "b" was created inside "a"
            val b = resolve(root, "a/b")
            assertIs<FileStorageEntry.Directory>(b)
            assertEquals("a", b.parentHandle.name)
            assertEquals("b", b.directoryHandle!!.name)
        }
    }

    @Test
    fun createDirsDoesNotOverrideInvalidPath() = runTest {
        runResolveTest { root ->
            root.getFileHandle("a", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/b", createDirs = true)
            assertIs<FileStorageEntry.InvalidPath>(result)
            assertEquals(root.name, result.parentHandle.name)
            assertEquals("a", result.name)
        }
    }

    @Test
    fun createDirsWithExistingPathReturnsFile() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getFileHandle("file.txt", fileSystemGetFileOptions(create = true))

            val result = resolve(root, "a/file.txt", createDirs = true)
            assertIs<FileStorageEntry.File>(result)
            assertEquals("a", result.parentHandle.name)
            assertEquals("file.txt", result.name)
            assertEquals("file.txt", result.fileHandle!!.name)
        }
    }

    @Test
    fun createDirsWithExistingPathReturnsDirectory() = runTest {
        runResolveTest { root ->
            val aDir = root.getDirectoryHandle("a", fileSystemGetDirectoryOptions(create = true))
            aDir.getDirectoryHandle("b", fileSystemGetDirectoryOptions(create = true))

            val result = resolve(root, "a/b", createDirs = true)
            assertIs<FileStorageEntry.Directory>(result)
            assertEquals("a", result.parentHandle.name)
            assertEquals("b", result.name)
            assertEquals("b", result.directoryHandle!!.name)
        }
    }
}