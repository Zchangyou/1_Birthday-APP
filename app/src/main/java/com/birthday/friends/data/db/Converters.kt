package com.birthday.friends.data.db

import androidx.room.TypeConverter
import com.birthday.friends.data.model.DateType
import com.birthday.friends.data.model.EventType

class Converters {

    @TypeConverter
    fun fromEventType(value: EventType): String = value.name

    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)

    @TypeConverter
    fun fromDateType(value: DateType): String = value.name

    @TypeConverter
    fun toDateType(value: String): DateType = DateType.valueOf(value)
}
