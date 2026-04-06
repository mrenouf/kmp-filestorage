package com.bitgrind.filestorage.test

import com.bitgrind.filestorage.FileStorage

expect suspend fun runFileStorageTest(block: suspend (FileStorage) -> Unit)
