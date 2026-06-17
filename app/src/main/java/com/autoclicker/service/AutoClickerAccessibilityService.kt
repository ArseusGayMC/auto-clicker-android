package com.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*
import kotlin.coroutines.resume

/**
 * AutoClickerAccessibilityService
 *
 * Sorumluluğu:
 *  – dispatchGesture() ile belirtilen koordinata dokunma hareketi gönderir.
 *  – Tıklama döngüsü Dispatchers.Default üzerinde çalışır → UI asla donmaz.
 *
 * Servis sisteme bağlandığında companion object'teki _instance referansı
 * doldurulur. FloatingButtonService bu referans üzerinden metotları çağırır;
 * startService / bindService kullanılmaz (AccessibilityService'i uygulama
 * başlatamaz, yalnızca sistem bağlayabilir).
 *
 * Thread yönetimi — neden Coroutine?
 *  • dispatchGesture callback'leri main thread'e gelir; coroutine suspend
 *    ederek bekler — hiçbir thread bloklanmaz.
 *  • Handler ile aynı sonuç elde edilebilir ancak cancel/scope yönetimi
 *    coroutine ile çok daha temizdir.
 */
class AutoClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClickerSvc"

        @Volatile
        private var _instance: AutoClickerAccessibilityService? = null

        /** Servis Ayarlar > Erişilebilirlik'ten etkinleştirilip bağlandıysa true döner. */
        fun isConnected(): Boolean = _instance != null

        /**
         * Belirtilen koordinata tıklamayı başlatır.
         * Servis bağlı değilse sessizce uyarı loglar.
         */
        fun startClicking(x: Float, y: Float, intervalMs: Long) {
            _instance?.beginClicking(x, y, intervalMs)
                ?: Log.w(TAG, "Servis bağlı değil — Ayarlar > Erişilebilirlik'ten etkinleştirin")
        }

        /** Devam eden tıklama döngüsünü durdurur. */
        fun stopClicking() {
            _instance?.endClicking()
        }
    }

    // SupervisorJob: bir tıklama hatası diğerlerini iptal etmez.
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var clickingJob: Job? = null

    // @Volatile: birden fazla thread (FloatingButtonService + clicking coroutine) okuyabilir.
    @Volatile private var targetX = 0f
    @Volatile private var targetY = 0f
    @Volatile private var intervalMs = 100L

    /* ─── Lifecycle ──────────────────────────────────────────────────── */

    override fun onServiceConnected() {
        _instance = this
        Log.d(TAG, "Bağlandı — canPerformGestures=${serviceInfo?.canPerformGestures()}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit   // Kullanılmıyor

    override fun onInterrupt() {
        endClicking()
    }

    override fun onDestroy() {
        endClicking()
        serviceScope.cancel()
        _instance = null
        super.onDestroy()
        Log.d(TAG, "Servis sonlandırıldı")
    }

    /* ─── Tıklama kontrolü ───────────────────────────────────────────── */

    private fun beginClicking(x: Float, y: Float, interval: Long) {
        targetX = x
        targetY = y
        intervalMs = interval.coerceAtLeast(50L)   // 50 ms minimum — çok hızlı gesture kuyruğu birikmez

        // Önceki döngüyü iptal et (sürükleme sırasında pozisyon güncellenir).
        clickingJob?.cancel()
        clickingJob = serviceScope.launch {
            Log.d(TAG, "Tıklama başladı → ($x, $y) her ${intervalMs}ms")
            while (isActive) {
                dispatchClick(targetX, targetY)
                delay(intervalMs)
            }
        }
    }

    private fun endClicking() {
        clickingJob?.cancel()
        clickingJob = null
        Log.d(TAG, "Tıklama durdu")
    }

    /* ─── Gesture gönderimi ──────────────────────────────────────────── */

    /**
     * Tek bir dokunma hareketi gönderir ve callback gelene kadar suspend eder.
     *
     * GestureDescription.StrokeDescription(path, startTime=0, duration=50ms):
     *  • duration 50 ms → uygulamalar bunu "tek dokunuş" olarak algılar.
     *  • Çok kısa (1 ms) olursa bazı uygulamalar kaçırabilir.
     *
     * dispatchGesture null dönerse (servis gesture izni yoksa) devam ederiz —
     * kullanıcı Accessibility ayarından servisi doğru yapılandırmamış demektir.
     */
    private suspend fun dispatchClick(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0L, 50L)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        suspendCancellableCoroutine { cont ->
            val dispatched = dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(Unit)
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    if (cont.isActive) cont.resume(Unit)
                }
            }, null)

            if (!dispatched) {
                Log.w(TAG, "dispatchGesture false döndü — canPerformGestures=false olabilir")
                if (cont.isActive) cont.resume(Unit)
            }
        }
    }
}
