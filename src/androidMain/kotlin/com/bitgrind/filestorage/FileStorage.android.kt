package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext

actual suspend fun getFileStorage(): FileStorage = MultiPlatformFileStorage(
    scope = CoroutineScope(CoroutineName("filestorage") + currentCoroutineContext())
)
