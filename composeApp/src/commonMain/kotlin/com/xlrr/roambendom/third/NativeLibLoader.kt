package com.xlrr.roambendom.third

/**
 * 平台相关的原生库加载。
 * JVM 和 Android 通过 System.loadLibrary 加载 ech_request，
 * 其他平台按需提供 actual 实现。
 */
expect fun loadEchRequestLibrary()
