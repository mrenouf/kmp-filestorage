@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

group = "com.bitgrind"
version = "0.1"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatform.library)
    alias(libs.plugins.dokka)
    id("maven-publish")
}


kotlin {
    android {
        namespace = "org.bitgrind.filestorage"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JVM_11
        }
    }
    jvm {
        compilerOptions {
            jvmTarget = JVM_11
        }
    }
    js {
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }
    wasmJs {
        browser()
        binaries.library()
        generateTypeScriptDefinitions()
        compilerOptions {
            target = "es2015"
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
        }
        webMain.dependencies {
            implementation(libs.kotlin.wrappers.browser)
            implementation(libs.kotlin.wrappers.js)
            implementation(libs.kotlin.wrappers.web)
            implementation(libs.kotlinx.browser)
            implementation(libs.kotlinx.coroutines.core)

        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)
            implementation(libs.kotlinx.coroutines.test)
        }
        webTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.wrappers.jsTest)
            implementation(libs.kotlinx.coroutines.test)
        }

    }
}
tasks.withType<Test> {
    useJUnitPlatform()
}
