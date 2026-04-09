package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.FileStorage

expect suspend fun runFileStorageTest(block: suspend (FileStorage, path: String) -> Unit)
