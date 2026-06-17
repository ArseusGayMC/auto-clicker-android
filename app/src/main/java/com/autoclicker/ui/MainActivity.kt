package com.autoclicker.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.autoclicker.R
import com.autoclicker.service.AutoClickerAccessibilityService
import com.autoclicker.service.FloatingButtonService
import com.autoclicker.viewmodel.AutoClickerViewModel
import kotlinx.coroutines.launch

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
                startFloatingButtonService()
                Toast.makeText(this, "Yüzen buton başlatıldı", Toast.LENGTH_SHORT).show()
            } else {
                showPermissionDialog()
            }
        }

        stopButton.setOnClickListener {
            stopFloatingButtonService()
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
            showPermissionDialog()
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        // Check SYSTEM_ALERT_WINDOW permission (Android 6+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Log.w(TAG, "SYSTEM_ALERT_WINDOW permission not granted")
                return false
            }
        }
        
        // Check if Accessibility Service is enabled
        return isAccessibilityServiceEnabled()
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = "${packageName}/${AutoClickerAccessibilityService::class.java.name}"
        val enabled = enabledServices.contains(serviceName)
        
        if (!enabled) {
            Log.w(TAG, "Accessibility Service not enabled")
        }
        
        return enabled
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting FloatingButtonService", e)
            Toast.makeText(this, "Hata: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        stopService(intent)
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("İzin Gereklidir")
            .setMessage("Uygulamanın çalışabilmesi için:\n\n" +
                    "1. Overlay İzni (Di\u011fer uygulamalar\u0131n üzerine görüntülenme)\n" +
                    "2. Accessibility Service (Eri\u015flebilirlik Hizmeti)\n\n" +
                    "Lütfen ayarlardan bu izinleri verin.")
            .setPositiveButton("Ayarlar") { _, _ ->
                openAccessibilitySettings()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Log.e(TAG, "Error opening accessibility settings", e)
            Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}