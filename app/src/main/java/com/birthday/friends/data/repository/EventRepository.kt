package com.birthday.friends.data.repository

import android.content.Context
import com.birthday.friends.data.db.AppDatabase
import com.birthday.friends.data.model.Event
import kotlinx.coroutines.flow.Flow

class EventRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).eventDao()

    val allEvents: Flow<List<Event>> = dao.getAllEvents()

    suspend fun insert(event: Event): Long = dao.insert(event)

    suspend fun update(event: Event) = dao.update(event)

    suspend fun delete(event: Event) = dao.delete(event)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun getById(id: Long): Event? = dao.getEventById(id)

    suspend fun getAllEventsWithDate(): List<Event> = dao.getAllEventsWithDate()

    suspend fun getAllEventsSync(): List<Event> = dao.getAllEventsSync()

    suspend fun insertAll(events: List<Event>) = dao.insertAll(events)
}
