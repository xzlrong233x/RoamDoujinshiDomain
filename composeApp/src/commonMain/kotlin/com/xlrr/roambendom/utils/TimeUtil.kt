package com.xlrr.roambendom.utils

import androidx.compose.ui.platform.LocalLocalization
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object TimeUtil {
    val CN_TIME_FORMATTER_SIMPLE by lazy {
        LocalDateTime.Format {
            year()
            char('年')
            monthNumber(Padding.NONE)
            char('月')
            day(Padding.NONE)
            char('日')
            hour(Padding.NONE)
            char('点')
            minute(Padding.NONE)
            char('分')
        }
    }

    val COMMON_TIME_FORMATTER by lazy {
        LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
            char(' ')
            hour()
            char(':')
            minute()
            char(':')
            second()
        }
    }

    fun formatTime(time: Long, pt: DateTimeFormat<LocalDateTime>) : String {
        return Instant.fromEpochMilliseconds(time)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(pt)
    }

    fun formatTime(time: Long) : String {
        // TODO: 做语言判断
        return formatTime(time, CN_TIME_FORMATTER_SIMPLE)
    }
}