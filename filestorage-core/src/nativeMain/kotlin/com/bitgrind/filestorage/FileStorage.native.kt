package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.MultiPlatformFileStorage

@Suppress("unused")
actual fun getFileStorage(): FileStorage = MultiPlatformFileStorage()