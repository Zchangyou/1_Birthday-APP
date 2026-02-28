package com.birthday.friends.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Event 数据模型。
 * 仅 name 为必填，其余字段全部可空（选填）。
 */
@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 姓名（必填） */
    val name: String,

    /** 事件类型：BIRTHDAY / ANNIVERSARY / CUSTOM */
    val eventType: EventType = EventType.BIRTHDAY,

    /** 日期类型：SOLAR（公历）/ LUNAR（农历） */
    val dateType: DateType = DateType.SOLAR,

    /** 月（1-12），可空表示未填日期 */
    val month: Int? = null,

    /** 日（1-31）*/
    val day: Int? = null,

    /** 年份，可空。填写后可算年龄/周年数 */
    val year: Int? = null,

    /** 手机号（选填） */
    val phone: String? = null,

    /** 身份证号（选填，加密存储） */
    val idCardEncrypted: String? = null,

    /** 籍贯（选填） */
    val hometown: String? = null,

    /** 家庭住址（选填） */
    val address: String? = null,

    /** 备注（选填） */
    val note: String? = null,

    /** 提前7天提醒（默认关） */
    val remind7Days: Boolean = false,

    /** 提前3天提醒（默认关） */
    val remind3Days: Boolean = false,

    /** 当天提醒（默认开） */
    val remindOnDay: Boolean = true,

    /** 创建时间戳 */
    val createdAt: Long = System.currentTimeMillis()
)

enum class EventType {
    BIRTHDAY,       // 生日
    ANNIVERSARY,    // 纪念日
    CUSTOM          // 自定义
}

enum class DateType {
    SOLAR,  // 公历
    LUNAR   // 农历
}
