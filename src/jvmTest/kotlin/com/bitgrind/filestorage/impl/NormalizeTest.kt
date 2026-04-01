package com.bitgrind.filestorage.impl

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

class NormalizeTest {
    companion object {
        @JvmStatic
        fun pathNormalizationCases(): Stream<Arguments> = Stream.of(
            Arguments.of("a",           listOf("a")),
            Arguments.of("a/b/c",       listOf("a", "b", "c")),
            Arguments.of("a//b///c",    listOf("a", "b", "c")),
            Arguments.of("a/./b",       listOf("a", "b")),
            Arguments.of("a/b",         listOf("a", "b")),
            Arguments.of("a//b/",       listOf("a", "b")),
            Arguments.of("a/b/..",      listOf("a")),
            Arguments.of("../a/b",      listOf("a", "b")),
            Arguments.of("/a/b/..",     listOf("a")),
            Arguments.of("/../a",       listOf("a")),
            Arguments.of("/",           emptyList<String>())
        )

        @JvmStatic
        fun pathParentCases(): Stream<Arguments> = Stream.of(
            Arguments.of("a/b/c",       listOf("a", "b")),
            Arguments.of("a//b///c",    listOf("a", "b")),
            Arguments.of("a/./b",       listOf("a")),
            Arguments.of("a//b/",       listOf("a")),
            Arguments.of("a/b/..",      emptyList<String>()),
            Arguments.of("../a/b",      listOf("a")),
            Arguments.of("/a/b/..",     emptyList<String>()),
            Arguments.of("/../a",       emptyList<String>()),
            Arguments.of("/",           emptyList<String>()),
        )
    }

    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("pathNormalizationCases")
    fun testNormalize(input: String, expected: List<String>) {
        assertEquals(expected, input.segments())
    }

    @ParameterizedTest(name = "{index}: \"{0}\" → {1}")
    @MethodSource("pathParentCases")
    fun testParent(input: String, expected: List<String>) {
        assertEquals(expected, input.segments().parent())
    }
}