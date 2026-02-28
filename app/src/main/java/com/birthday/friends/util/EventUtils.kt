package com.birthday.friends.util

import com.birthday.friends.data.model.DateType
import com.birthday.friends.data.model.Event
import com.birthday.friends.data.model.EventType
import com.birthday.friends.lunar.LunarConverter
import java.util.Calendar

/**
 * 事件工具类：计算剩余天数、显示文案等。
 */
object EventUtils {

    /**
     * 计算今年该事件距今还有多少天。
     * 返回 0 表示今天，正数表示未来，负数表示今年已过（用下一年）。
     * 若 Event 没有日期信息，返回 null。
     */
    fun getDaysUntilNextOccurrence(event: Event): Int? {
        val month = event.month ?: return null
        val day = event.day ?: return null

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val targetCal = getNextOccurrenceCalendar(event, today)
        val diffMs = targetCal.timeInMillis - today.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * 获取下一次发生日期的 Calendar。
     * 若今年日期已过，则取下一年。
     */
    fun getNextOccurrenceCalendar(event: Event, from: Calendar = Calendar.getInstance()): Calendar {
        val month = event.month ?: return Calendar.getInstance()
        val day = event.day ?: return Calendar.getInstance()
        val thisYear = from.get(Calendar.YEAR)

        val thisYearCal = resolveToSolar(event.dateType, month, day, thisYear)
        // 处理公历 2 月 29 日平年问题（resolveToSolar 内部已处理）

        val fromNormalized = from.clone() as Calendar
        fromNormalized.set(Calendar.HOUR_OF_DAY, 0)
        fromNormalized.set(Calendar.MINUTE, 0)
        fromNormalized.set(Calendar.SECOND, 0)
        fromNormalized.set(Calendar.MILLISECOND, 0)

        return if (thisYearCal >= fromNormalized) {
            thisYearCal
        } else {
            resolveToSolar(event.dateType, month, day, thisYear + 1)
        }
    }

    /**
     * 将农历或公历日期解析为指定年份的公历 Calendar。
     */
    fun resolveToSolar(dateType: DateType, month: Int, day: Int, year: Int): Calendar {
        return if (dateType == DateType.LUNAR) {
            LunarConverter.lunarToSolar(month, day, year)
        } else {
            // 公历，处理 2 月 29 日平年
            val cal = Calendar.getInstance()
            cal.set(year, month - 1, 1, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(maxDay))
            cal
        }
    }

    /**
     * 格式化剩余天数文字：今天 / N天后 / 已过（不显示）
     */
    fun formatDaysText(days: Int): String = when {
        days == 0 -> "今天"
        days > 0 -> "${days}天后"
        else -> "已过${-days}天"
    }

    /**
     * 格式化事件日期显示文字（如"3月15日（公历）" 或 "农历八月初八"）
     */
    fun formatDateLabel(event: Event): String {
        val month = event.month ?: return "未填日期"
        val day = event.day ?: return "未填日期"
        return if (event.dateType == DateType.LUNAR) {
            val lunarMonthNames = arrayOf("", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
            val lunarDayNames = arrayOf(
                "", "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
            )
            "农历${lunarMonthNames.getOrElse(month) { month.toString() }}月${lunarDayNames.getOrElse(day) { day.toString() }}"
        } else {
            "${month}月${day}日（公历）"
        }
    }

    /**
     * 计算年龄或周年数（需要 year 字段）。
     * 比较今年该日期与今天，决定是否已过生日。
     */
    fun calcAge(event: Event): Int? {
        val birthYear = event.year ?: return null
        val month = event.month ?: return null
        val day = event.day ?: return null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val thisYear = today.get(Calendar.YEAR)
        val thisYearOccurrence = resolveToSolar(event.dateType, month, day, thisYear)
        return if (thisYearOccurrence > today) {
            thisYear - birthYear - 1  // 今年生日/纪念日尚未到来
        } else {
            thisYear - birthYear      // 今天或今年已过
        }
    }

    /**
     * 事件类型中文名
     */
    fun eventTypeLabel(type: EventType): String = when (type) {
        EventType.BIRTHDAY -> "生日"
        EventType.ANNIVERSARY -> "纪念日"
        EventType.CUSTOM -> "自定义"
    }
}
