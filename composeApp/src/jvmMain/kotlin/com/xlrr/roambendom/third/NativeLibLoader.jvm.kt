package com.xlrr.roambendom.third

import java.io.File

actual fun loadEchRequestLibrary() { //TODO：优化AI代码（？
    // 至少可以发现在资源中的dll不可通过loadLibrary加载
    val osName = System.getProperty("os.name").lowercase()
    val libName = when {
        osName.contains("win")  -> "ech_request.dll"
        osName.contains("mac")  -> "libech_request.dylib"
        else                    -> "libech_request.so"
    }

    // 策略1: java.library.path (Gradle run / 打包产物)
    try {
        System.loadLibrary("ech_request")
        return
    } catch (_: UnsatisfiedLinkError) {
    }

    // 策略2: 从 classpath 资源提取 (JAR 内嵌)
    val classLoader = Thread.currentThread().contextClassLoader
        ?: ClassLoader.getSystemClassLoader()
    val resourceStream = classLoader.getResourceAsStream("native/$libName")
    if (resourceStream != null) {
        val tempFile = File.createTempFile("ech_request_", ".${libName.substringAfterLast('.')}")
            .also { it.deleteOnExit() }
        resourceStream.use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output) }
        }
        System.load(tempFile.absolutePath)
        return
    }

    // 策略3: 从工作目录的多条候选路径加载
    val userDir = System.getProperty("user.dir") ?: "."
    val candidates = listOf(
        "composeApp/src/jvmMain/resources/native/$libName",
        "composeApp/build/native_libs/$libName",
        "src/jvmMain/resources/native/$libName",
        "build/native_libs/$libName",
        "app/$libName",
        "app/native/$libName",
        "runtime/bin/$libName",
        libName,
    )
    for (relPath in candidates) {
        val file = File(userDir, relPath)
        if (file.exists()) {
            System.load(file.absolutePath)
            return
        }
    }

    throw UnsatisfiedLinkError("ech_request not found")
}