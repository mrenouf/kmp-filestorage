package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope

actual fun getFileStorage(context: CoroutineContext): FileStorage = MultiPlatformFileStorage(
    scope = CoroutineScope(CoroutineName("filestorage") + context)
)
