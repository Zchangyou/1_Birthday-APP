package com.birthday.friends.ui.contacts

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthday.friends.data.repository.EventRepository
import com.birthday.friends.ui.main.EventUiItem
import com.birthday.friends.util.EventUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.Collator
import java.util.Locale

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)
    private val _query = MutableStateFlow("")

    val filteredEvents = combine(repo.allEvents, _query) { events, q ->
        events
            .filter { e ->
                q.isEmpty() ||
                    e.name.contains(q, ignoreCase = true) ||
                    (e.phone?.contains(q) == true)
            }
            .sortedWith(Comparator { a, b ->
                Collator.getInstance(Locale.CHINESE).compare(a.name, b.name)
            })
            .map { e ->
                val days = EventUtils.getDaysUntilNextOccurrence(e)
                EventUiItem(
                    event = e,
                    daysUntil = days,
                    dateLabel = EventUtils.formatDateLabel(e),
                    daysText = if (days != null) EventUtils.formatDaysText(days) else "未填日期",
                    ageOrAnniversary = EventUtils.calcAge(e)
                )
            }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun setQuery(q: String) {
        _query.value = q
    }
}
