package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.ByteReader
import com.bitgrind.filestorage.impl.SourceReader
import kotlinx.io.buffered

actual fun byteReaderOf(vararg chunks: ByteArray): ByteReader {
    return SourceReader(ChunkedRawSource(chunks.toList()).buffered())
}
