package com.autoclicker.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.autoclicker.R
import com.autoclicker.service.AutoClickerAccessibilityService
import com.autoclicker.viewmodel.AutoClickerViewModel
import kotlinx.coroutines.launch
import android.os.Bundle

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: AutoClickerViewModel
    private lateinit var accessibilityManager: AccessibilityManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this).get(AutoClickerViewModel::class.java)
        accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

        setupUI()
        collectSettings()
        checkPermissions()
    }

    private fun setupUI() {
        val startButton = findViewById<Button>(R.id.btn_start)
        val stopButton = findViewById<Button>(R.id.btn_stop)
        val intervalSeekBar = findViewById<SeekBar>(R.id.seekbar_interval)
        val intervalText = findViewById<TextView>(R.id.tv_interval)

        startButton.setOnClickListener {
            if (hasRequiredPermissions()) {
                viewModel.startFloatingButton()
                Toast.makeText(this, "Yüzen buton başlatıldı", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissions()
            }
        }

        stopButton.setOnClickListener {
            viewModel.stopFloatingButton()
            Toast.makeText(this, "Yüzen buton durduruldu", Toast.LENGTH_SHORT).show()
        }

        intervalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val interval = (progress + 50).toLong()
                intervalText.text = "Tıklama Aralığı: ${interval}ms"
                viewModel.updateClickInterval(interval)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun collectSettings() {
        lifecycleScope.launch {
            viewModel.settings.collect { settings ->
                // UI update if needed
            }
        }
    }

    private fun checkPermissions() {
        if (!hasRequiredPermissions()) {
            requestPermissions()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                return false
            }
        }
        return isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(
            "${packageName}/${AutoClickerAccessibilityService::class.java.name}"
        )
    }

    private fun requestPermissions() {
        AlertDialog.Builder(this)
            .setTitle("İzin Gereklidir")
            .setMessage("Accessibility ve Overlay izinleri gereklidir.\n\n" +
                    "1. Overlay İzni\n" +
                    "2. Accessibility Service'i etkinleştir")
            .setPositiveButton("Ayarları Aç") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}