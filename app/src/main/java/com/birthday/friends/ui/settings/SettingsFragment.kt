package com.birthday.friends.ui.settings

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.birthday.friends.alarm.AlarmScheduler
import com.birthday.friends.alarm.DailyCheckWorker
import com.birthday.friends.alarm.NotificationTestHelper
import com.birthday.friends.backup.BackupManager
import com.birthday.friends.data.repository.EventRepository
import com.birthday.friends.databinding.FragmentSettingsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val exportLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val result = BackupManager.exportToUri(requireContext(), uri)
            result.onSuccess { count ->
                Toast.makeText(requireContext(), "已导出 $count 条记录", Toast.LENGTH_SHORT).show()
            }.onFailure {
                Toast.makeText(requireContext(), "导出失败：${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@registerForActivityResult
        lifecycleScope.launch {
            val result = BackupManager.importFromUri(requireContext(), uri)
            result.onSuccess { count ->
                Toast.makeText(requireContext(), "已导入 $count 条记录", Toast.LENGTH_SHORT).show()
                // 重新注册所有 Alarm
                DailyCheckWorker.enqueue(requireContext())
            }.onFailure {
                Toast.makeText(requireContext(), "导入失败：${it.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)

        // 显示当前提醒时间
        fun updateTimeLabel() {
            val hour = prefs.getInt("reminder_hour", 9)
            val minute = prefs.getInt("reminder_minute", 0)
            binding.tvReminderTime.text = String.format("%02d:%02d", hour, minute)
        }
        updateTimeLabel()

        binding.rowReminderTime.setOnClickListener {
            val hour = prefs.getInt("reminder_hour", 9)
            val minute = prefs.getInt("reminder_minute", 0)
            TimePickerDialog(requireContext(), { _, h, m ->
                prefs.edit().putInt("reminder_hour", h).putInt("reminder_minute", m).apply()
                updateTimeLabel()
                Toast.makeText(requireContext(), "提醒时间已保存", Toast.LENGTH_SHORT).show()
                // 立即用新时间重新调度所有已有事件的 Alarm
                lifecycleScope.launch {
                    val repo = EventRepository(requireContext())
                    val events = repo.getAllEventsWithDate()
                    events.forEach { event ->
                        AlarmScheduler.cancelAll(requireContext(), event)
                        AlarmScheduler.scheduleAll(requireContext(), event)
                    }
                }
            }, hour, minute, true).show()
        }

        // 导出数据
        binding.rowExport.setOnClickListener {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val filename = "birthday_backup_${sdf.format(Calendar.getInstance().time)}.json"
            exportLauncher.launch(filename)
        }

        // 导入数据
        binding.rowImport.setOnClickListener {
            importLauncher.launch(arrayOf("application/json", "*/*"))
        }

        // 电池优化白名单引导
        binding.rowBatteryOptimization.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${requireContext().packageName}")
                }
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }

        // 荣耀/华为专属：受保护应用设置
        binding.rowHonorProtected.setOnClickListener {
            openHonorProtectedAppsSetting()
        }

        // 测试通知
        binding.rowTestNotification.setOnClickListener {
            NotificationTestHelper.sendTestNow(requireContext())
            Toast.makeText(requireContext(), "测试通知已发送，请查看通知栏", Toast.LENGTH_SHORT).show()
        }

        // 检测是否荣耀/华为设备，若不是则隐藏荣耀专属设置项
        val brand = Build.BRAND.lowercase()
        if (!brand.contains("honor") && !brand.contains("huawei")) {
            binding.rowHonorProtected.visibility = View.GONE
        }
    }

    private fun openHonorProtectedAppsSetting() {
        val intents = listOf(
            // EMUI 荣耀
            Intent().apply {
                setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            },
            // 备用：应用启动管理
            Intent().apply {
                setClassName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"
                )
            }
        )
        for (intent in intents) {
            try {
                startActivity(intent)
                return
            } catch (e: Exception) {
                continue
            }
        }
        Toast.makeText(requireContext(), "请手动在手机管家 → 启动管理中允许本应用自启动", Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
