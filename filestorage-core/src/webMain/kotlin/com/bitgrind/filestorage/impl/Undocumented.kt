@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.impl

import js.promise.Promise
import js.promise.await
import web.fs.FileSystemDirectoryHandle
import web.fs.FileSystemFileHandle
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js
import kotlin.js.unsafeCast

@RequiresOptIn("Undocumented OPFS API")
annotation class OpfsUndocumented

/**
 * Stub for the non-standard OPFS `move()` API not yet included in kotlin-wrappers.
 *
 * `FileSystemHandle.move()` is part of the File System Access API Level 2 proposal,
 * available in Chromium-based browsers but not yet ratified in the spec or exposed
 * by kotlin-wrappers.
 *
 * - GitHub MDN Issue: [`api.FileSystemFileHandle.move` available in Chrome #26876](https://github.com/mdn/browser-compat-data/issues/26876)
 * - GitHub WhatWG PR: [Add the `FileSystemFileHandle.move` method #180](https://github.com/whatwg/fs/pull/180/changes)
 */
@OpfsUndocumented
internal suspend fun FileSystemFileHandle.move(
    directory: FileSystemDirectoryHandle,
    newName: String,
) {
    jsFileHandleMove(this, directory, newName).unsafeCast<Promise<JsAny?>>().await()
}

@OpfsUndocumented
private fun jsFileHandleMove(
    @Suppress("unused") handle: JsAny,
    @Suppress("unused") directory: FileSystemDirectoryHandle,
    @Suppress("unused") newName: String,
): JsAny = js("handle.move(directory, newName)")
