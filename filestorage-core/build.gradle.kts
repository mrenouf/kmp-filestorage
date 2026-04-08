@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11

group = "com.bitgrind"
version = "0.1"

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.dokka)
    id("maven-publish")
}

kotlin {
    androidTarget()
    linuxX64()
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
            implementation(libs.kotlinx.coroutines.core)
        }
        webMain.dependencies {
            implementation(libs.kotlin.wrappers.browser)
            implementation(libs.kotlin.wrappers.js)
            implementation(libs.kotlin.wrappers.web)
            implementation(libs.kotlinx.browser)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter.api)
            implementation(libs.junit.jupiter.params)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.bitgrind.filestorage.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
