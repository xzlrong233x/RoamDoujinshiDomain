package com.xlrr.roambendom.config

import kotlin.concurrent.Volatile

/**
 * 由桌面端的 WindowState 同步写入；其余平台保持默认 true。
 */
object AppVisibility {
    @Volatile
    var visible: Boolean = true
}
