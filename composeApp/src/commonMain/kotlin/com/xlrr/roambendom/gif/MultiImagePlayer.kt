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

    fun generalDraw(drawFunc: (T) -> Unit) {
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
            val bit = map.firstNotNullOf {
                if (dr <= it.value.milliseconds) {
                    it.key
                } else {
                    dr -= it.value.milliseconds
                    null
                }
            }
            drawFunc(bit)
            if (newLp) {
                if (loopCount == -1 || (loopCount in 1..loop)) {
                    isRunning = false
                }
            }
        }
    }
}