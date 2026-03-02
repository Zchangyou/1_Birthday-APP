package com.birthday.friends.ui.edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.birthday.friends.alarm.AlarmScheduler
import com.birthday.friends.data.model.DateType
import com.birthday.friends.data.model.EventType
import com.birthday.friends.databinding.FragmentEditBinding
import com.birthday.friends.util.EncryptionUtil
import kotlinx.coroutines.launch

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditViewModel by viewModels()
    private var editingId: Long? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        // 接收编辑模式传入的 eventId
        editingId = arguments?.getLong("eventId")?.takeIf { it != 0L }
        if (editingId != null) {
            viewModel.loadEvent(editingId!!)
        }

        setupSpinners()
        observeEvent()
        setupSaveButton()
    }

    private fun setupSpinners() {
        // 事件类型下拉
        val eventTypes = arrayOf("生日", "纪念日", "自定义")
        val eventTypeAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            eventTypes
        )
        eventTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEventType.adapter = eventTypeAdapter

        // 日期类型切换（公历/农历）
        binding.rgDateType.setOnCheckedChangeListener { _, checkedId ->
            // 切换日期选择器提示文字
            when (checkedId) {
                binding.rbSolar.id -> {
                    binding.etMonth.hint = "公历月（1-12）"
                    binding.etDay.hint = "公历日（1-31）"
                }
                binding.rbLunar.id -> {
                    binding.etMonth.hint = "农历月（1-12）"
                    binding.etDay.hint = "农历日（1-30）"
                }
            }
        }
    }

    private fun observeEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.event.collect { event ->
                    event ?: return@collect
                    binding.etName.setText(event.name)
                    binding.spinnerEventType.setSelection(
                        when (event.eventType) {
                            EventType.BIRTHDAY -> 0
                            EventType.ANNIVERSARY -> 1
                            EventType.CUSTOM -> 2
                        }
                    )
                    if (event.dateType == DateType.LUNAR) {
                        binding.rbLunar.isChecked = true
                    } else {
                        binding.rbSolar.isChecked = true
                    }
                    event.month?.let { binding.etMonth.setText(it.toString()) }
                    event.day?.let { binding.etDay.setText(it.toString()) }
                    event.year?.let { binding.etYear.setText(it.toString()) }
                    event.phone?.let { binding.etPhone.setText(it) }
                    EncryptionUtil.decrypt(event.idCardEncrypted)?.let {
                        binding.etIdCard.setText(it)
                    }
                    event.hometown?.let { binding.etHometown.setText(it) }
                    event.address?.let { binding.etAddress.setText(it) }
                    event.note?.let { binding.etNote.setText(it) }
                    binding.switchRemind7.isChecked = event.remind7Days
                    binding.switchRemind3.isChecked = event.remind3Days
                    binding.switchRemindToday.isChecked = event.remindOnDay
                }
            }
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "姓名不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val eventType = when (binding.spinnerEventType.selectedItemPosition) {
                1 -> EventType.ANNIVERSARY
                2 -> EventType.CUSTOM
                else -> EventType.BIRTHDAY
            }
            val dateType = if (binding.rbLunar.isChecked) DateType.LUNAR else DateType.SOLAR
            val month = binding.etMonth.text.toString().toIntOrNull()
            val day = binding.etDay.text.toString().toIntOrNull()
            val year = binding.etYear.text.toString().toIntOrNull()

            // 简单校验日期合法性
            if (month != null && (month < 1 || month > 12)) {
                Toast.makeText(requireContext(), "月份应在 1-12 之间", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (day != null && (day < 1 || day > 31)) {
                Toast.makeText(requireContext(), "日期应在 1-31 之间", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.saveEvent(
                id = editingId,
                name = name,
                eventType = eventType,
                dateType = dateType,
                month = month,
                day = day,
                year = year,
                phone = binding.etPhone.text.toString(),
                idCard = binding.etIdCard.text.toString(),
                hometown = binding.etHometown.text.toString(),
                address = binding.etAddress.text.toString(),
                note = binding.etNote.text.toString(),
                remind7 = binding.switchRemind7.isChecked,
                remind3 = binding.switchRemind3.isChecked,
                remindToday = binding.switchRemindToday.isChecked
            ) { savedId ->
                // 重新注册 Alarm
                viewLifecycleOwner.lifecycleScope.launch {
                    val repo = com.birthday.friends.data.repository.EventRepository(requireContext())
                    val saved = repo.getById(savedId)
                    if (saved != null) {
                        AlarmScheduler.cancelAll(requireContext(), saved)
                        AlarmScheduler.scheduleAll(requireContext(), saved)
                    }
                    findNavController().navigateUp()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
