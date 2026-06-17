package com.autoclicker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import com.autoclicker.ui.MainActivity
import kotlin.math.abs

/**
 * FloatingButtonService — Ön planda çalışan servis.
 *
 * WindowManager ile ekranın üzerine 64 dp'lik bir buton çizer.
 * Butonun dokunma davranışı:
 *
 *   ACTION_DOWN  → Tıklama döngüsü butonun merkezi koordinatında başlar.
 *   ACTION_MOVE  → Parmak DRAG_THRESHOLD_PX piksel üzeri hareket ederse buton
 *                  sürüklenir; tıklama hedefi butonla birlikte güncellenir.
 *   ACTION_UP    → Tıklama döngüsü durur.
 *
 * FLAG_NOT_FOCUSABLE + FLAG_NOT_TOUCH_MODAL sayesinde butonun dışına yapılan
 * dokunuşlar alttaki uygulamaya geçer — kullanıcı tıklarken uygulamayla
 * etkileşime devam edebilir.
 *
 * AccessibilityService ile iletişim:
 *   startService / Intent kullanılmaz. AutoClickerAccessibilityService.companion
 *   referansı üzerinden doğrudan metot çağrısı yapılır.
 */
class FloatingButtonService : Service() {

    companion object {
        private const val TAG               = "FloatingButtonSvc"
        private const val CHANNEL_ID        = "auto_clicker_channel"
        private const val NOTIFICATION_ID   = 1001
        private const val BUTTON_SIZE_DP    = 64
        private const val DRAG_THRESHOLD_PX = 12

        /**
         * ViewModel / MainActivity tarafından güncellenir.
         * Tıklama başladığında o anki değer alınır.
         */
        @Volatile
        var clickIntervalMs: Long = 100L
    }

    private lateinit var windowManager: WindowManager
    private var floatingButton: ImageButton? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // Dokunma durumu
    private var touchStartRawX = 0f
    private var touchStartRawY = 0f
    private var paramStartX    = 0
    private var paramStartY    = 0
    private var isDragging     = false

    /* ─── Lifecycle ──────────────────────────────────────────────────── */

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        createOverlayButton()
        Log.d(TAG, "Servis başladı")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AutoClickerAccessibilityService.stopClicking()
        removeOverlayButton()
        super.onDestroy()
        Log.d(TAG, "Servis sonlandırıldı")
    }

    /* ─── Foreground bildirim ────────────────────────────────────────── */

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto Clicker",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Auto Clicker yüzen butonu aktif"
            setShowBadge(false)
            enableLights(false)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openPi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, FloatingButtonService::class.java).also { it.action = "STOP" },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Clicker Aktif")
            .setContentText("Yüzen butona basılı tutun → tıklama başlar, bırakın → durur")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(openPi)
            .addAction(android.R.drawable.ic_delete, "Kapat", stopPi)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /* ─── Overlay butonu ─────────────────────────────────────────────── */

    private fun createOverlayButton() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val sizePx = (BUTTON_SIZE_DP * resources.displayMetrics.density).toInt()

        floatingButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundColor(Color.parseColor("#CC6200EE"))   // Yarı saydam mor
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setPadding(14, 14, 14, 14)
        }

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // FLAG_NOT_FOCUSABLE  : buton klavye odağı almaz.
            // FLAG_NOT_TOUCH_MODAL: buton dışındaki dokunuşlar alttaki uygulamaya geçer.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 400
        }

        windowManager.addView(floatingButton, layoutParams)
        setupTouchListener()
        Log.d(TAG, "Overlay buton oluşturuldu (${sizePx}x${sizePx}px)")
    }

    private fun removeOverlayButton() {
        try {
            floatingButton?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "removeView hatası", e)
        }
        floatingButton = null
    }

    /* ─── Dokunma mantığı ────────────────────────────────────────────── */

    private fun setupTouchListener() {
        floatingButton?.setOnTouchListener { _, event ->
            val params = layoutParams ?: return@setOnTouchListener false
            val btn    = floatingButton ?: return@setOnTouchListener false

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    // Başlangıç pozisyonlarını kaydet
                    touchStartRawX = event.rawX
                    touchStartRawY = event.rawY
                    paramStartX    = params.x
                    paramStartY    = params.y
                    isDragging     = false

                    // Tıklamayı buton merkezinden başlat
                    startClickingAtCenter(params)
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - touchStartRawX
                    val dy = event.rawY - touchStartRawY

                    // Eşiği geçtiyse sürükleme moduna geç
                    if (!isDragging &&
                        (abs(dx) > DRAG_THRESHOLD_PX || abs(dy) > DRAG_THRESHOLD_PX)
                    ) {
                        isDragging = true
                    }

                    if (isDragging) {
                        params.x = paramStartX + dx.toInt()
                        params.y = paramStartY + dy.toInt()
                        try {
                            windowManager.updateViewLayout(btn, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "updateViewLayout hatası", e)
                        }
                        // Buton hareket ederken tıklama hedefini güncelle
                        startClickingAtCenter(params)
                    }
                    true
                }

                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    isDragging = false
                    AutoClickerAccessibilityService.stopClicking()
                    Log.d(TAG, "Tıklama durduruldu")
                    true
                }

                else -> false
            }
        }
    }

    /**
     * Butonun merkez koordinatını hesaplayıp AccessibilityService'e iletir.
     * Buton hareket ettiğinde tekrar çağrılır — AccessibilityService eski
     * döngüyü iptal edip yeni koordinatla yeniden başlatır.
     */
    private fun startClickingAtCenter(params: WindowManager.LayoutParams) {
        val sizePx = (BUTTON_SIZE_DP * resources.displayMetrics.density)
        val cx = params.x + sizePx / 2f
        val cy = params.y + sizePx / 2f
        AutoClickerAccessibilityService.startClicking(cx, cy, clickIntervalMs)
    }
}
