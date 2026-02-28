package com.birthday.friends.backup

import android.content.Context
import android.net.Uri
import com.birthday.friends.data.model.DateType
import com.birthday.friends.data.model.Event
import com.birthday.friends.data.model.EventType
import com.birthday.friends.data.repository.EventRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * JSON 格式数据导出/导入。
 * 使用 SAF（Storage Access Framework），兼容 Android 10+。
 * 身份证号以明文存储在备份文件中（用户自行保管文件安全性）。
 */
object BackupManager {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /** 导出所有事件到指定 URI（用户通过 SAF 选择保存位置） */
    suspend fun exportToUri(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val repo = EventRepository(context)
                val events = repo.getAllEventsSync()
                val dtoList = events.map { it.toDto(context) }
                val json = gson.toJson(dtoList)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                Result.success(events.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /** 从指定 URI 导入事件（跳过同名重复） */
    suspend fun importFromUri(context: Context, uri: Uri): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                } ?: return@withContext Result.failure(Exception("无法读取文件"))

                val type = object : TypeToken<List<EventDto>>() {}.type
                val dtoList: List<EventDto> = gson.fromJson(json, type)
                val events = dtoList.map { it.toEvent(context) }

                val repo = EventRepository(context)
                repo.insertAll(events)
                Result.success(events.size)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

/** 用于 JSON 序列化的数据传输对象（明文存储身份证号）*/
data class EventDto(
    val name: String,
    val eventType: String,
    val dateType: String,
    val month: Int?,
    val day: Int?,
    val year: Int?,
    val phone: String?,
    val idCard: String?,         // 明文，备份文件中存明文
    val hometown: String?,
    val address: String?,
    val note: String?,
    val remind7Days: Boolean,
    val remind3Days: Boolean,
    val remindOnDay: Boolean
)

private fun Event.toDto(context: Context): EventDto {
    val idCard = com.birthday.friends.util.EncryptionUtil.decrypt(idCardEncrypted)
    return EventDto(
        name = name,
        eventType = eventType.name,
        dateType = dateType.name,
        month = month,
        day = day,
        year = year,
        phone = phone,
        idCard = idCard,
        hometown = hometown,
        address = address,
        note = note,
        remind7Days = remind7Days,
        remind3Days = remind3Days,
        remindOnDay = remindOnDay
    )
}

private fun EventDto.toEvent(context: Context): Event {
    val encryptedIdCard = com.birthday.friends.util.EncryptionUtil.encrypt(idCard)
    return Event(
        name = name,
        eventType = runCatching { EventType.valueOf(eventType) }.getOrDefault(EventType.BIRTHDAY),
        dateType = runCatching { DateType.valueOf(dateType) }.getOrDefault(DateType.SOLAR),
        month = month,
        day = day,
        year = year,
        phone = phone,
        idCardEncrypted = encryptedIdCard,
        hometown = hometown,
        address = address,
        note = note,
        remind7Days = remind7Days,
        remind3Days = remind3Days,
        remindOnDay = remindOnDay
    )
}
