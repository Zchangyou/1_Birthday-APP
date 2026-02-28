package com.birthday.friends.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.birthday.friends.R
import com.birthday.friends.alarm.AlarmScheduler
import com.birthday.friends.databinding.FragmentDetailBinding
import com.birthday.friends.util.EncryptionUtil
import com.birthday.friends.util.EventUtils
import kotlinx.coroutines.launch

class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val eventId = arguments?.getLong("eventId") ?: return
        viewModel.loadEvent(eventId)

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    event ?: return@collect

                    binding.tvName.text = event.name
                    binding.tvEventType.text = EventUtils.eventTypeLabel(event.eventType)
                    binding.tvDate.text = EventUtils.formatDateLabel(event)

                    val days = EventUtils.getDaysUntilNextOccurrence(event)
                    binding.tvDays.text = if (days != null) EventUtils.formatDaysText(days) else "未填日期"

                    val age = EventUtils.calcAge(event)
                    binding.tvAge.text = when {
                        age != null && days == 0 -> "今天${age}周岁 / ${age}周年"
                        age != null -> "届时${age}周岁 / ${age}周年"
                        else -> ""
                    }

                    // 显示选填字段（非空才显示）
                    binding.rowPhone.visibility = if (!event.phone.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.tvPhone.text = event.phone

                    val idCard = EncryptionUtil.decrypt(event.idCardEncrypted)
                    binding.rowIdCard.visibility = if (!idCard.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.tvIdCard.text = idCard

                    binding.rowHometown.visibility = if (!event.hometown.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.tvHometown.text = event.hometown

                    binding.rowAddress.visibility = if (!event.address.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.tvAddress.text = event.address

                    binding.rowNote.visibility = if (!event.note.isNullOrEmpty()) View.VISIBLE else View.GONE
                    binding.tvNote.text = event.note

                    binding.tvReminders.text = buildString {
                        if (event.remindOnDay) append("当天  ")
                        if (event.remind3Days) append("提前3天  ")
                        if (event.remind7Days) append("提前7天")
                        if (isEmpty()) append("无提醒")
                    }

                    binding.btnEdit.setOnClickListener {
                        findNavController().navigate(
                            R.id.action_detail_to_edit,
                            bundleOf("eventId" to event.id)
                        )
                    }

                    binding.btnDelete.setOnClickListener {
                        AlertDialog.Builder(requireContext())
                            .setTitle("删除")
                            .setMessage("确定要删除「${event.name}」吗？")
                            .setPositiveButton("删除") { _, _ ->
                                AlarmScheduler.cancelAll(requireContext(), event)
                                viewModel.delete(event) {
                                    findNavController().navigateUp()
                                }
                            }
                            .setNegativeButton("取消", null)
                            .show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
