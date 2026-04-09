@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import js.buffer.ArrayBufferOptions
import web.encoding.TextDecodeOptions
import web.fs.FileSystemCreateWritableOptions
import web.fs.FileSystemGetDirectoryOptions
import web.fs.FileSystemGetFileOptions
import web.fs.FileSystemRemoveOptions
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.js

internal fun fileSystemGetFileOptions(
    @Suppress("unused") create: Boolean
): FileSystemGetFileOptions = js("({ 'create': create })")

internal fun fileSystemGetDirectoryOptions(
    @Suppress("unused") create: Boolean
): FileSystemGetDirectoryOptions = js("({ 'create': create })")

internal fun fileSystemRemoveOptions(
    @Suppress("unused") recursive: Boolean
): FileSystemRemoveOptions = js("({ 'recursive': recursive })")

internal fun fileSystemCreateWritableOptions(
    @Suppress("unused") append: Boolean
): FileSystemCreateWritableOptions = js("({ 'keepExistingData': append })")

fun arrayBufferOptions(
    @Suppress("unused") maxByteLength: Int
): ArrayBufferOptions = js("({ maxByteLength: maxByteLength })")

fun textDecodeOptions(
    @Suppress("unused") stream: Boolean
): TextDecodeOptions = js("({ stream: stream })")
