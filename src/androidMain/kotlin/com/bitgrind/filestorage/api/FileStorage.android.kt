package com.bitgrind.filestorage.api

import com.bitgrind.filestorage.api.FileStorage
import com.bitgrind.filestorage.impl.MultiPlatformFileStorage

actual fun getFileStorage(): FileStorage = MultiPlatformFileStorage()
