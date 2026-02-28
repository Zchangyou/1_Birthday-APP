package com.birthday.friends.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.birthday.friends.data.model.Event
import com.birthday.friends.data.repository.EventRepository
import com.birthday.friends.util.EventUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EventUiItem(
    val event: Event,
    val daysUntil: Int?,          // null 表示没有日期
    val dateLabel: String,         // 如 "3月15日（公历）" 或 "农历八月初八"
    val daysText: String,          // 如 "今天" / "3天后" / "已过"
    val ageOrAnniversary: Int?     // 年龄或周年数
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = EventRepository(application)

    /** 全部事件，按剩余天数排序（今天置顶，无日期的排最后）*/
    val events = repo.allEvents
        .map { list -> list.toUiItems().sortedBy { it.daysUntil ?: Int.MAX_VALUE } }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun delete(event: Event) {
        viewModelScope.launch {
            repo.delete(event)
        }
    }

    private fun List<Event>.toUiItems(): List<EventUiItem> = map { event ->
        val days = EventUtils.getDaysUntilNextOccurrence(event)
        EventUiItem(
            event = event,
            daysUntil = days,
            dateLabel = EventUtils.formatDateLabel(event),
            daysText = if (days != null) EventUtils.formatDaysText(days) else "未填日期",
            ageOrAnniversary = EventUtils.calcAge(event)
        )
    }
}
