# kmp-filestorage

A Kotlin Multiplatform file storage library providing a unified API for file I/O across JVM, Android, and web (JS/WASM) targets. Write once, store everywhere.

## Features

- **Unified API** — the same `FileStorage`, `ByteReader`, and `ByteWriter` interfaces work on every supported platform
- **Structured I/O** — built-in support for reading and writing `Int`, `Short`, `String`, and `ByteArray` with a consistent big-endian wire format
- **Cross-platform compatibility** — data written on JVM can be read in a browser and vice versa
- **Async-first** — all operations are `suspend` functions, integrating naturally with coroutines
- **Web storage via OPFS** — on JS/WASM targets, uses the browser's [Origin Private File System](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API/Origin_private_file_system) for persistent, sandboxed storage

## Platform Support

| Platform    | Storage Backend               |
|-------------|-------------------------------|
| JVM         | `kotlinx.io.SystemFileSystem` |
| Android     | `kotlinx.io.SystemFileSystem` |
| JavaScript  | Origin Private File System (OPFS) |
| WebAssembly | Origin Private File System (OPFS) |

## Usage

### Get a `FileStorage` instance

```kotlin
val fs = getFileStorage()
```

The `getFileStorage()` function is an `expect`/`actual` entry point — each platform returns the appropriate implementation automatically.

### Write data

```kotlin
val writer = fs.getWriter("mydir/data.bin", append = false)
writer.writeInt(42)
writer.writeString("hello, world")
writer.writeByteArray(byteArrayOf(0x01, 0x02, 0x03))
writer.close()
```

### Read data back

```kotlin
val reader = fs.getReader("mydir/data.bin")
val number  = reader.readInt()       // 42
val message = reader.readString()    // "hello, world"
val bytes   = reader.readByteArray() // [0x01, 0x02, 0x03]
reader.close()
```

### File system operations

```kotlin
// Create directories (recursive)
fs.createDirectories("a/b/c")

// Check existence
if (fs.exists("a/b/c/file.bin")) { ... }

// Delete a file or directory
fs.delete("a/b/c/file.bin", recursive = false)
fs.delete("a/b", recursive = true)
```

## Wire Format

All values are encoded with a stable, cross-platform binary format:

| Type        | Encoding                                |
|-------------|-----------------------------------------|
| `Short`     | 2 bytes, big-endian                     |
| `Int`       | 4 bytes, big-endian                     |
| `String`    | UInt16 byte-length prefix + UTF-8 bytes |
| `ByteArray` | UInt16 element-count prefix + raw bytes |

This ensures that files written on one platform are always readable on another.

## Path Normalization

Paths are automatically normalized before use:

```
"a/b/../c"  →  a/c
"/a/b/."    →  a/b
"/../a"     →  a      (back-references above root are silently ignored)
```

## Adding to Your Project

This library is not yet published to Maven Central. To use it locally, include it as a composite build:

**`settings.gradle.kts`** (in your consuming project):
```kotlin
includeBuild("../kmp-filestorage")
```

**`build.gradle.kts`**:
```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.bitgrind:kmp-filestorage:0.1")
        }
    }
}
```

## Dependencies

| Library              | Version   | Purpose                         |
|----------------------|-----------|---------------------------------|
| Kotlin Multiplatform | 2.3.20    | Language & multiplatform tooling |
| kotlinx-io           | 0.9.0     | Cross-platform I/O primitives   |
| kotlinx-coroutines   | 1.10.2    | Suspend/async support           |
| kotlin-wrappers      | 2026.3.16 | Browser API bindings (web only) |

## License

This project is provided as-is. License TBD.