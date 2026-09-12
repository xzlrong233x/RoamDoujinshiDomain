import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.kotlinSerializaton)
}

// ── Rust 原生库构建 ──────────────────────────────────────────────

val rustProjectDir = file("${rootProject.projectDir}/plugin/ech-request")
val rustReleaseDir = file("${rustProjectDir}/target/release")
val nativeLibsDir      = file("${layout.buildDirectory.get().asFile}/native_libs")
val androidJniLibsDir  = file("${projectDir}/src/androidMain/jniLibs")

/** 编译 Rust 原生库 (cdylib → .dll / .so / .dylib) */
val buildRustLib by tasks.registering(Exec::class) {
    group       = "rust"
    description = "cargo build --release 编译 ech-request 原生库"
    workingDir  = rustProjectDir
    commandLine("cargo", "build", "--release")

    inputs.dir("${rustProjectDir}/src")
    inputs.file("${rustProjectDir}/Cargo.toml")
    outputs.dir(rustReleaseDir)
}

/** 将 Rust DLL 复制到 JVM desktop 可加载目录 */
val copyRustLibToJvm by tasks.registering(Copy::class) {
    group       = "rust"
    description = "复制 ech_request 原生库到 JVM native_libs"
    dependsOn(buildRustLib)

    from(rustReleaseDir) {
        include("ech_request.dll", "libech_request.so", "libech_request.dylib")
    }
    into(nativeLibsDir)
}

val jvmResourcesNativeDir = file("${projectDir}/src/jvmMain/resources/native")

/** 将 DLL 复制到 src/jvmMain/resources/native/，IDE 和 Gradle 均可见 */
val copyRustLibToJvmResources by tasks.registering(Copy::class) {
    group       = "rust"
    description = "复制 ech_request 到 JVM source resources (IDE 直接可用)"
    dependsOn(buildRustLib)

    from(rustReleaseDir) {
        include("ech_request.dll", "libech_request.so", "libech_request.dylib")
    }
    into(jvmResourcesNativeDir)
}

// ── Android 交叉编译（cargo-ndk） ─────────────────────────────────

// 手动检测 NDK（避免 AGP 的 android.ndkDirectory 在某些版本不可靠）
val sdkDir: String = run {
    // 从 local.properties 读取 sdk.dir（处理 Java properties 转义）
    val propsFile = file("../local.properties")
    val fromProps = if (propsFile.exists()) {
        propsFile.readLines()
                .firstOrNull { it.trimStart().startsWith("sdk.dir") }
                ?.substringAfter("=")
                ?.trim()?.replace("\\:", ":")?.replace("\\\\", "\\") // 反转义
    } else null
    fromProps
        ?: System.getenv("ANDROID_HOME")
        ?: System.getenv("ANDROID_SDK_ROOT")
        ?: ""
}

val ndkBase = file("$sdkDir/ndk")
val ndkVersion: String = if (ndkBase.exists()) {
    ndkBase.listFiles()?.filter { it.isDirectory }?.maxOfOrNull { it.name } ?: ""
} else ""

val ndkAvailable: Boolean = ndkVersion.isNotEmpty()

if (ndkAvailable) {
    val ndkDir = file("$sdkDir/ndk/$ndkVersion")

    /** 安装 Rust Android targets（如缺失） */
    val installRustAndroidTargets by tasks.registering(Exec::class) {
        group       = "rust"
        description = "rustup target add (Android ABIs)"
        commandLine("rustup", "target", "add",
            "aarch64-linux-android",
            "armv7-linux-androideabi",
            "x86_64-linux-android",
            "i686-linux-android")
    }

    /** 用 cargo-ndk 为 Android 交叉编译 ech-request */
    val buildRustLibAndroid by tasks.registering(Exec::class) {
        group       = "rust"
        description = "cargo ndk 交叉编译 ech-request (Android)"
        dependsOn(buildRustLib, installRustAndroidTargets)
        workingDir  = rustProjectDir
        environment("ANDROID_NDK_HOME", ndkDir.absolutePath)
        commandLine("cargo", "ndk",
            "-t", "arm64-v8a",
            "-t", "armeabi-v7a",
            "-t", "x86",
            "-t", "x86_64",
            "-o", androidJniLibsDir.absolutePath,
            "build", "--release")
        inputs.dir("${rustProjectDir}/src")
        inputs.file("${rustProjectDir}/Cargo.toml")
        outputs.dir(androidJniLibsDir)
    }

    // Android 构建前交叉编译
    tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
        .configureEach { dependsOn(buildRustLibAndroid) }
} else {
    throw GradleException(
        buildString {
            appendLine("Android NDK 未安装，跳过 ech-request Android 交叉编译。")
            appendLine("请在 Android Studio → SDK Manager → SDK Tools 中安装 NDK (Side by side)。")
        }
    )
}

// JVM desktop: 确保运行时原生库已就绪
tasks.matching { it.name == "jvmProcessResources" }.configureEach {
    dependsOn(copyRustLibToJvm, copyRustLibToJvmResources)
}
tasks.matching { it.name.startsWith("compile") && it.name.contains("Jvm") }.configureEach {
    dependsOn(copyRustLibToJvm, copyRustLibToJvmResources)
}
tasks.matching { it.name == "jvmJar" }.configureEach {
    dependsOn(copyRustLibToJvm, copyRustLibToJvmResources)
}

// ── Kotlin Multiplatform ────────────────────────────────────────

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.brower)
            implementation(libs.coil.gif)
        }
        commonMain.dependencies {
            implementation(libs.gifkt)
            implementation(libs.gifkt.compose)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.okhttp)
            implementation(libs.filekit.coil)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.core)
            implementation(libs.zoomable)
            implementation(libs.ksoup)
            implementation(libs.data.saver.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "com.xlrr.roambendom"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    ndkVersion = "30.0.14904198"

    defaultConfig {
        applicationId = "com.xlrr.roambendom"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "1.2.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            storeFile = File(projectDir.path + "/keystore/androidkey.jks")
            storePassword = "114514"
            keyAlias = "key114"
            keyPassword = "1919810"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(libs.compose.uiTooling)
}

// ── Desktop 应用配置 ────────────────────────────────────────────

compose.desktop {
    application {

        mainClass = "com.xlrr.roambendom.MainKt"

        buildTypes.release.proguard {
            configurationFiles.from("proguard-rules.pro")
            isEnabled = false
        // 似乎有点问题，开启proguard会使软件打开时报
        // io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider not found
        // 不知原因，可能是AI代码发力了
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "RoamDoujinshiDomain"
            packageVersion = "1.2.0"
        }
    }
}