package com.bitgrind.filestorage.impl

import web.fs.FileSystemCreateWritableOptions
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemGetFileOptions
import web.fs.FileSystemRemoveOptions
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@Suppress("unused")
@ExperimentalWasmJsInterop
internal fun fileSystemGetFileOptions(create: Boolean): FileSystemGetFileOptions = js("({ 'create': create })")

@Suppress("unused")
@ExperimentalWasmJsInterop
internal fun fileSystemGetDirectoryOptions(create: Boolean): FileSystemGetDirectoryOptions = js("({ 'create': create })")

@Suppress("unused")
@ExperimentalWasmJsInterop
internal fun fileSystemRemoveOptions(recursive: Boolean): FileSystemRemoveOptions = js("({ 'recursive': recursive })")

@Suppress("unused")
@ExperimentalWasmJsInterop
internal fun fileSystemCreateWritableOptions(append: Boolean): FileSystemCreateWritableOptions = js("({ 'keepExistingData': append })")