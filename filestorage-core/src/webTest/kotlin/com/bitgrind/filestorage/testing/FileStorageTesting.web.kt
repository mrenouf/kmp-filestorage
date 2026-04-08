@file:OptIn(ExperimentalWasmJsInterop::class)

package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.impl.OpfsByteReader
import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import js.typedarrays.toUint8Array
import web.streams.ReadableStream
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsArray
import kotlin.js.js
import kotlin.js.toJsArray

internal fun readableStreamOf(
    @Suppress("unused") array: JsArray<Uint8Array<ArrayBuffer>>
): ReadableStream<Uint8Array<ArrayBuffer>> = js("""
    new ReadableStream({
        start(ctrl) {
            for (var i = 0; i < array.length; i++) {
                ctrl.enqueue(array[i]);
            }
            ctrl.close();
        }
    })
""")


actual fun byteReaderOf(vararg chunks: ByteArray): ByteReader {
    val arr = chunks.map { it.toUint8Array() }.toJsArray()
    val stream = readableStreamOf(arr)
    return OpfsByteReader(stream.getReader())
}
