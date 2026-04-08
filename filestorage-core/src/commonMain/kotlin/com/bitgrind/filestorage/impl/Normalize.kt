package com.bitgrind.filestorage.impl


internal fun List<String>.parent(): List<String> {
    if (size < 2) return emptyList()
    return dropLast(1)
}

internal fun List<String>.target(): String = last()

internal fun String.segments(): List<String> {
    val stack = ArrayDeque<String>()
    for (part in split('/')) {
        when (part) {
            "", "." -> {
                // skip
            }
            ".." -> {
                if (stack.isNotEmpty()) {
                    stack.removeLast()
                }
            }
            else -> stack.addLast(part)
        }
    }
    return stack.toList()
}

internal fun String.absolute(separator: String): String {
    return this.segments().joinToString(prefix = separator, separator = separator)
}
