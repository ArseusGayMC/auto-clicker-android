package com.autoclicker.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.autoclicker.R
import com.autoclicker.service.AutoClickerAccessibilityService
import com.autoclicker.service.FloatingButtonService
import com.autoclicker.viewmodel.AutoClickerViewModel

class MainActivity : AppCompatActivity() {

    private companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var viewModel: AutoClickerViewModel

    // Overlay izni — Settings.ACTION_MANAGE_OVERLAY_PERMISSION dönüşünde durumu yenile
    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            updatePermissionStatus()
        }

    // Android 13+ bildirim izni
    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            granted -> Log.d(TAG, "Bildirim izni: $granted")
        }

    /* ─── Lifecycle ──────────────────────────────────────────────────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[AutoClickerViewModel::class.java]

        requestNotificationPermissionIfNeeded()
        setupUI()
        updatePermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        // Accessibility ayarlarından döndükten sonra durumu güncelle
        updatePermissionStatus()
    }

    /* ─── UI kurulumu ────────────────────────────────────────────────── */

    private fun setupUI() {
        val btnStart          = findViewById<Button>(R.id.btn_start)
        val btnStop           = findViewById<Button>(R.id.btn_stop)
        val seekBar           = findViewById<SeekBar>(R.id.seekbar_interval)
        val tvInterval        = findViewById<TextView>(R.id.tv_interval)
        val btnOverlay        = findViewById<Button>(R.id.btn_grant_overlay)
        val btnAccessibility  = findViewById<Button>(R.id.btn_grant_accessibility)

        // Başlat — her iki izin de verilmişse floating button servisini başlat
        btnStart.setOnClickListener {
            when {
                !hasOverlayPermission() -> {
                    Toast.makeText(this, "Önce Overlay iznini verin", Toast.LENGTH_SHORT).show()
                    requestOverlayPermission()
                }
                !isAccessibilityEnabled() -> {
                    Toast.makeText(this, "Önce Accessibility servisini etkinleştirin", Toast.LENGTH_LONG).show()
                    openAccessibilitySettings()
                }
                else -> {
                    viewModel.startFloatingButton()
                    Toast.makeText(this, "Yüzen buton başlatıldı ✓", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Durdur
        btnStop.setOnClickListener {
            viewModel.stopFloatingButton()
            Toast.makeText(this, "Yüzen buton durduruldu", Toast.LENGTH_SHORT).show()
        }

        // İzin butonları
        btnOverlay.setOnClickListener { requestOverlayPermission() }
        btnAccessibility.setOnClickListener { openAccessibilitySettings() }

        // Tıklama aralığı — 50ms … 500ms
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                val interval = (progress + 50).toLong()
                tvInterval.text = "Tıklama Aralığı: ${interval}ms"
                viewModel.updateClickInterval(interval)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }

    /* ─── İzin kontrolü ──────────────────────────────────────────────── */

    private fun hasOverlayPermission(): Boolean =
        Settings.canDrawOverlays(this)

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val svcName = "$packageName/${AutoClickerAccessibilityService::class.java.name}"
        return enabled.contains(svcName)
    }

    private fun requestOverlayPermission() {
        overlayPermissionLauncher.launch(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
        )
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Log.e(TAG, "Accessibility ayarları açılamadı", e)
            Toast.makeText(this, "Ayarlar açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /** İzin durumunu ekrana yansıt ve Başlat butonunu etkinleştir/devre dışı bırak. */
    private fun updatePermissionStatus() {
        val tvOverlay       = findViewById<TextView>(R.id.tv_overlay_status)
        val tvAccessibility = findViewById<TextView>(R.id.tv_accessibility_status)
        val btnStart        = findViewById<Button>(R.id.btn_start)

        val overlayOk        = hasOverlayPermission()
        val accessibilityOk  = isAccessibilityEnabled()

        tvOverlay.text =
            if (overlayOk) "✅ Overlay izni verildi"
            else           "❌ Overlay izni gerekli"

        tvAccessibility.text =
            if (accessibilityOk) "✅ Accessibility Servisi aktif"
            else                 "❌ Accessibility Servisi gerekli"

        // Her iki izin de varsa Başlat butonu aktif
        btnStart.isEnabled = overlayOk && accessibilityOk
        btnStart.alpha = if (btnStart.isEnabled) 1f else 0.5f
    }
}
