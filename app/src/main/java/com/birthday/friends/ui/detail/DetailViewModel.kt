package com.birthday.friends.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthday.friends.data.model.Event
import com.birthday.friends.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)

    private val _event = MutableStateFlow<Event?>(null)
    val event: StateFlow<Event?> = _event

    fun loadEvent(id: Long) {
        viewModelScope.launch {
            _event.value = repo.getById(id)
        }
    }

    fun delete(event: Event, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.delete(event)
            onDone()
        }
    }
}
