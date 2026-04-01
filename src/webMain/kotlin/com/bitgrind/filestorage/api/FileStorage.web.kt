package com.bitgrind.filestorage.api

import com.bitgrind.filestorage.impl.OpfsFileStorage

@Suppress("unused")
actual fun getFileStorage(): FileStorage = OpfsFileStorage()
