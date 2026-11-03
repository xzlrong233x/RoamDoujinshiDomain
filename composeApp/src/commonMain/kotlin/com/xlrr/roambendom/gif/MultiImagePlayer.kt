package com.xlrr.roambendom.gif

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

open class MultiImagePlayer<T>(
    val map: Map<T, Int>,
    val loopCount: Int
) {
    protected var invalidateTick by mutableIntStateOf(0) // 太伟大了，COMPOSE

    protected var startTimestamp = Clock.System.now()
    var isRunning = true
        protected set
    protected var loop = 0
    protected val totalDuration = map.values.sumOf { it }.milliseconds

    open fun close() {}

    fun generalDraw(drawFunc: (T) -> Unit) {
        // 空帧表 / 零总时长会让下面的取模 while 永远减不到 0，必须在入口拦掉
        if (map.isEmpty() || totalDuration <= 0.milliseconds) {
            isRunning = false
            return
        }
        var dr = Clock.System.now() - startTimestamp
        var lp = 0
        var newLp = false
        while (dr > totalDuration) {
            dr -= totalDuration
            lp++
        }
        if (lp != loop) {
            loop = lp
            newLp = true
        }
        if (isRunning) {
            // 帧区间是 [start, start+duration)：dr == duration 属于下一帧；
            // 全部帧都吃不下 dr 时（正好等于总时长的边界）兜底取最后一帧。
            var bit: T? = null
            for ((frame, durationMs) in map) {
                val duration = durationMs.milliseconds
                if (dr < duration) {
                    bit = frame
                    break
                }
                dr -= duration
            }
            val chosen = bit ?: map.keys.lastOrNull() ?: run {
                isRunning = false
                return
            }
            drawFunc(chosen)
            if (newLp) {
                if (loopCount == -1 || (loopCount in 1..loop)) {
                    isRunning = false
                }
            }
        }
    }
}