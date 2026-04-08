package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.OpfsFileStorage
import web.navigator.navigator

@Suppress("unused")
actual fun getFileStorage(): FileStorage = OpfsFileStorage(storage = navigator.storage)
