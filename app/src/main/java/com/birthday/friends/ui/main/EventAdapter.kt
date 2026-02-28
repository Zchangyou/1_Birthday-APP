package com.birthday.friends.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.birthday.friends.R
import com.birthday.friends.databinding.ItemEventBinding
import com.birthday.friends.util.EventUtils

class EventAdapter(
    private val onClick: (EventUiItem) -> Unit,
    private val onLongClick: (EventUiItem) -> Boolean
) : ListAdapter<EventUiItem, EventAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val binding: ItemEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: EventUiItem) {
            binding.tvName.text = item.event.name
            binding.tvEventType.text = EventUtils.eventTypeLabel(item.event.eventType)
            binding.tvDays.text = item.daysText
            binding.tvDate.text = item.dateLabel

            // 年龄/周年数显示
            if (item.ageOrAnniversary != null) {
                binding.tvAge.text = when {
                    item.event.year != null && item.daysUntil == 0 ->
                        "今年${item.ageOrAnniversary}周岁"
                    item.event.year != null ->
                        "届时${item.ageOrAnniversary}周岁"
                    else -> ""
                }
            } else {
                binding.tvAge.text = ""
            }

            // 今天高亮
            if (item.daysUntil == 0) {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.card_today)
                )
            } else {
                binding.root.setCardBackgroundColor(
                    binding.root.context.getColor(R.color.card_normal)
                )
            }

            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener { onLongClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<EventUiItem>() {
            override fun areItemsTheSame(old: EventUiItem, new: EventUiItem) =
                old.event.id == new.event.id

            override fun areContentsTheSame(old: EventUiItem, new: EventUiItem) =
                old == new
        }
    }
}
