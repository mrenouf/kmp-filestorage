package com.bitgrind.filestorage.impl

import js.buffer.ArrayBuffer
import js.typedarrays.Uint8Array
import web.encoding.TextDecoder
import web.encoding.TextEncoder

internal object Utf8 {
    private val decoder = TextDecoder()
    private val encoder = TextEncoder()

    fun decode(bytes: Uint8Array<ArrayBuffer>): String = decoder.decode(bytes)
    fun encode(string: String): Uint8Array<ArrayBuffer> = encoder.encode(string)
}