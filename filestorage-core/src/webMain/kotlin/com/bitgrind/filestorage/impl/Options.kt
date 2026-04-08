package com.bitgrind.filestorage.impl

import js.buffer.ArrayBufferOptions
import web.encoding.TextDecodeOptions
import web.fs.FileSystemCreateWritableOptions
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemGetFileOptions
import web.fs.FileSystemRemoveOptions
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

@ExperimentalWasmJsInterop
internal fun fileSystemGetFileOptions(
    @Suppress("unused") create: Boolean
): FileSystemGetFileOptions = js("({ 'create': create })")

@ExperimentalWasmJsInterop
internal fun fileSystemGetDirectoryOptions(
    @Suppress("unused") create: Boolean
): FileSystemGetDirectoryOptions = js("({ 'create': create })")

@ExperimentalWasmJsInterop
internal fun fileSystemRemoveOptions(
    @Suppress("unused") recursive: Boolean
): FileSystemRemoveOptions = js("({ 'recursive': recursive })")

@ExperimentalWasmJsInterop
internal fun fileSystemCreateWritableOptions(
    @Suppress("unused") append: Boolean
): FileSystemCreateWritableOptions = js("({ 'keepExistingData': append })")

@ExperimentalWasmJsInterop
fun arrayBufferOptions(
    @Suppress("unused") maxByteLength: Int
): ArrayBufferOptions = js("({ maxByteLength: maxByteLength })")

@OptIn(ExperimentalWasmJsInterop::class)
fun textDecodeOptions(
    @Suppress("unused") stream: Boolean
): TextDecodeOptions = js("({ stream: stream })")
