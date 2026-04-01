package com.bitgrind.filestorage.api

import com.bitgrind.filestorage.impl.MultiPlatformFileStorage

@Suppress("unused")
actual fun getFileStorage(): FileStorage = MultiPlatformFileStorage()
