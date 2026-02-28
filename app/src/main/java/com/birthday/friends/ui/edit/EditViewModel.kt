package com.birthday.friends.ui.edit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthday.friends.data.model.DateType
import com.birthday.friends.data.model.Event
import com.birthday.friends.data.model.EventType
import com.birthday.friends.data.repository.EventRepository
import com.birthday.friends.util.EncryptionUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EditViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _event.value = repo.getById(id)
        }
    }

    fun saveEvent(
        id: Long?,
        name: String,
        eventType: EventType,
        dateType: DateType,
        month: Int?,
        day: Int?,
        year: Int?,
        phone: String?,
        idCard: String?,
        hometown: String?,
        address: String?,
        note: String?,
        remind7: Boolean,
        remind3: Boolean,
        remindToday: Boolean,
        onResult: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val encryptedIdCard = EncryptionUtil.encrypt(idCard?.takeIf { it.isNotBlank() })
            val event = Event(
                id = id ?: 0L,
                name = name,
                eventType = eventType,
                dateType = dateType,
                month = month,
                day = day,
                year = year,
                phone = phone?.takeIf { it.isNotBlank() },
                idCardEncrypted = encryptedIdCard,
                hometown = hometown?.takeIf { it.isNotBlank() },
                address = address?.takeIf { it.isNotBlank() },
                note = note?.takeIf { it.isNotBlank() },
                remind7Days = remind7,
                remind3Days = remind3,
                remindOnDay = remindToday
            )
            val savedId = if (id != null && id != 0L) {
                repo.update(event)
                id
            } else {
                repo.insert(event)
            }
            onResult(savedId)
        }
    }
}
