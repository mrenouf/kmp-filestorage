package com.bitgrind.filestorage

import com.bitgrind.filestorage.impl.MultiPlatformFileStorage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext

@Suppress("unused")
actual fun getFileStorage(context: CoroutineContext): FileStorage = MultiPlatformFileStorage(
    scope = CoroutineScope(CoroutineName("filestorage") + context)
)