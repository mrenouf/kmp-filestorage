package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.OpfsFileStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import web.navigator.navigator
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
actual fun getFileStorage(context: CoroutineContext): FileStorage = OpfsFileStorage(
    storage = navigator.storage,
    scope = CoroutineScope(CoroutineName("filestorage") + context),
)
