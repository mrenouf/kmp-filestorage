# kmp-filestorage

A Kotlin Multiplatform file storage library providing a unified API for file I/O across JVM, Android, and web (JS/WASM) targets. Write once, store everywhere.

## Features

- **Unified API** — one interface across mobile, desktop and web
- **Structured I/O** — familiar interface modeled after `Source` and `Sync`, supporting standard Kotlin types `Byte`, `Short`, `Int`, `Long`, `String` and `ByteArray`
- **Async-first** — all operations are `suspend` functions, integrating naturally with coroutines
- **Web storage via OPFS** — on JS/WASM targets, uses the browser's [Origin Private File System](https://developer.mozilla.org/en-US/docs/Web/API/File_System_API/Origin_private_file_system) for persistent, sandboxed storage

## Platform Support

| Platform     | Storage Backend               |
|--------------|-------------------------------|
| JVM          | `kotlinx.io.SystemFileSystem` |
| Android      | `kotlinx.io.SystemFileSystem` |
| Native       | `kotlinx.io.SystemFileSystem` |
| Js/Browser   | Origin Private File System (OPFS) |
| Wasm/Browser | Origin Private File System (OPFS) |

> Note: The Kotlin/Js implementation of `kotlinx.io.FileSystem` [supports only server side 
> applications](https://kotlinlang.org/api/kotlinx-io/kotlinx-io-core/kotlinx.io.files/-system-file-system.html).

## Usage

### Get a `FileStorage` instance

```kotlin
val fs = getFileStorage()
```

### Bulk read and write text files

```kotlin
fs.writeText("notes.txt", "hello, world")
fs.readText("notes.txt")
```

### Bulk read and write binary files

```kotlin
fs.writeBytes("data.bin", byteArrayOf(1, 2, 3))
fs.readBytes("data.bin")
```

### Write structured data

```kotlin
val writer = fs.getWriter("cache/data.bin", append = false)
writer.writeInt(42)
writer.writeInt(12)
writer.writeString("hello, world")
writer.writeInt(3)
writer.writeByteArray(byteArrayOf(0x01, 0x02, 0x03))
writer.close()
```

### Read data back

```kotlin
val reader = fs.getReader("cache/data.bin")
val number  = reader.readInt()               // 42
val textLength = reader.readInt()
val message = reader.readString(textLength)  // "hello, world"
val dataLength = reader.readInt()
val bytes = reader.readByteArray(dataLength) // [0x01, 0x02, 0x03]
reader.close()
```

### File system operations

```kotlin
// Create directories (recursive)
fs.createDirectories("a/b/c")

// Check existence
if (fs.exists("a/b/c/file.bin")) {   }

// Delete a file or directory
fs.delete("a/b/c/file.bin", recursive = false)
fs.delete("a/b", recursive = true)
```

## Path Handling and Normalization

Web targets have no concept of 'current working directory'. On Js and WasmJs targets, relative paths are always interpreted against the root (`/`). The path `.` is removed when normalizing paths. On other platforms, path references work as expected.
This allows relative paths relative to the `CWD` on JVM and Native targets (`./data/cache`) to be interpreted as (`/data/cache`) within the browser file system.

Paths are normalized, removing `.` and resolving `..` against parent directories.

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
            implementation("com.bitgrind:filestorage-core:0.1")
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