package com.birthday.friends

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.birthday.friends.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // Notification denied — app still works, just no push
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15 (targetSdk 35) Edge-to-edge
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = binding.bottomNav
        bottomNav.setupWithNavController(navController)

        // 子页面（详情/编辑）隐藏底部导航，主标签页显示
        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = when (destination.id) {
                R.id.mainFragment, R.id.contactsFragment, R.id.settingsFragment -> View.VISIBLE
                else -> View.GONE
            }
        }

        // Ensure the root view doesn't overlap with system UI at the top
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Ensure BottomNavigationView doesn't get obscured by the system navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(bottomNav) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(bottom = bars.bottom)
            insets
        }

        requestPermissionsIfNeeded()
        checkBatteryOptimization()
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        // Android 12+：引导用户授权「精确闹钟」权限，否则提醒可能大幅延迟
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
                if (!prefs.getBoolean("alarm_perm_guide_shown", false)) {
                    prefs.edit().putBoolean("alarm_perm_guide_shown", true).apply()
                    AlertDialog.Builder(this)
                        .setTitle("需要「精确闹钟」权限")
                        .setMessage("请在系统设置中允许本应用设置精确提醒，否则生日提醒可能大幅延迟或不触发。")
                        .setPositiveButton("去设置") { _, _ ->
                            try {
                                startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:$packageName")
                                    }
                                )
                            } catch (e: Exception) {
                                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:$packageName")
                                })
                            }
                        }
                        .setNegativeButton("稍后", null)
                        .show()
                }
            }
        }
    }

    private fun checkBatteryOptimization() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val shown = prefs.getBoolean("battery_guide_shown", false)
            if (!shown) {
                prefs.edit().putBoolean("battery_guide_shown", true).apply()
                showBatteryOptimizationDialog()
            }
        }
    }

    private fun showBatteryOptimizationDialog() {
        AlertDialog.Builder(this)
            .setTitle("重要：允许后台运行")
            .setMessage("为确保生日提醒准时到达，请将本应用加入电池优化白名单（不受限制），否则关闭 APP 后提醒可能无法推送。")
            .setPositiveButton("去设置") { _, _ ->
                try {
                    // Try direct battery optimization settings
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    // Fallback to general battery settings
                    startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
            .setNegativeButton("稍后再说", null)
            .show()
    }
}
