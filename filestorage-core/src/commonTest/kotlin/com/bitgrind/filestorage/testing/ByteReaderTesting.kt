package com.bitgrind.filestorage.testing

import com.bitgrind.filestorage.ByteReader

expect fun byteReaderOf(vararg chunks: ByteArray): ByteReader