package com.birthday.friends.lunar

import com.nlf.calendar.Lunar
import com.nlf.calendar.Solar
import java.util.Calendar

/**
 * 农历 ↔ 公历换算工具。
 * 使用 lunar-java 库（com.github.6tail:lunar-java）。
 */
object LunarConverter {

    /**
     * 将农历 月/日 换算为指定年份的公历日期。
     * @param lunarMonth 农历月（1-12）
     * @param lunarDay   农历日（1-30）
     * @param targetYear 目标公历年份
     * @return 对应的公历日期（Calendar），若该年无对应农历日期则回退到最近有效日期
     */
    fun lunarToSolar(lunarMonth: Int, lunarDay: Int, targetYear: Int): Calendar {
        return try {
            // 先获取当年农历正月初一的公历日期，再推算
            val lunar = Lunar(targetYear, lunarMonth, lunarDay)
            val solar = lunar.solar
            Calendar.getInstance().apply {
                set(solar.year, solar.month - 1, solar.day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            // 若农历日期在目标年份不存在（如闰月），回退到同月最后一天
            fallbackLunarDate(lunarMonth, lunarDay, targetYear)
        }
    }

    /**
     * 回退策略：若指定农历日期不存在（如当年无闰月），
     * 尝试当月最后一天，若仍失败则用正月对应日期。
     */
    private fun fallbackLunarDate(lunarMonth: Int, lunarDay: Int, targetYear: Int): Calendar {
        // 尝试当月最后一天（29 天月份）
        for (d in lunarDay downTo 1) {
            try {
                val lunar = Lunar(targetYear, lunarMonth, d)
                val solar = lunar.solar
                return Calendar.getInstance().apply {
                    set(solar.year, solar.month - 1, solar.day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
            } catch (e: Exception) {
                continue
            }
        }
        // 最终回退到公历当年的同月同日（尽量不失去提醒）
        return Calendar.getInstance().apply {
            set(targetYear, lunarMonth - 1, lunarDay.coerceAtMost(28), 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    /**
     * 将公历日期转换为农历描述文字，如"农历三月初八"。
     */
    fun solarToLunarString(year: Int, month: Int, day: Int): String {
        return try {
            val solar = Solar(year, month, day)
            val lunar = solar.lunar
            "农历${lunar.monthInChinese}月${lunar.dayInChinese}"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * 获取今年农历对应的公历日期（用于计算主页剩余天数）。
     */
    fun getThisYearSolarDate(lunarMonth: Int, lunarDay: Int): Calendar {
        val thisYear = Calendar.getInstance().get(Calendar.YEAR)
        return lunarToSolar(lunarMonth, lunarDay, thisYear)
    }
}
