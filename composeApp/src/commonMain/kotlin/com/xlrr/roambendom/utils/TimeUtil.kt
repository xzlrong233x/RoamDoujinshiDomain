package com.xlrr.roambendom.utils

import androidx.compose.material3.SelectableDates
import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import roambendom.composeapp.generated.resources.Res
import roambendom.composeapp.generated.resources.now_sys_lang
import kotlin.time.Clock
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

    val PIXIV_PARAM_FORMATTER by lazy {
        LocalDateTime.Format {
            year()
            char('-')
            monthNumber()
            char('-')
            day()
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

    val DATE_PICK_RANGE = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= Clock.System.now().toEpochMilliseconds()
        override fun isSelectableYear(year: Int): Boolean = year <= Clock.System.now().toLocalDateTime(TimeZone.UTC).year
    }

    fun formatTime(time: Long, pt: DateTimeFormat<LocalDateTime>) : String {
        return Instant.fromEpochMilliseconds(time)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(pt)
    }

    @Composable
    fun formatTime(time: Long) : String {
        val lang = stringResource(Res.string.now_sys_lang)
        if (lang == "cn") {
            return formatTime(time, CN_TIME_FORMATTER_SIMPLE)
        }
        return formatTime(time, COMMON_TIME_FORMATTER)
    }
}