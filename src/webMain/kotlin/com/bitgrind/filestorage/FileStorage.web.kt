package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.OpfsFileStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import web.navigator.navigator

@Suppress("unused")
actual suspend fun getFileStorage(): FileStorage = OpfsFileStorage(
    storage = navigator.storage,
    scope = CoroutineScope(CoroutineName("filestorage") + currentCoroutineContext())
)
