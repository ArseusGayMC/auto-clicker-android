package com.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AutoClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClicker"
        private const val MIN_CLICK_INTERVAL = 50L
        const val ACTION_START_CLICKING = "START_CLICKING"
        const val ACTION_STOP_CLICKING = "STOP_CLICKING"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var clickingJob: Job? = null
    private var isClicking = false
    private var clickX = 0f
    private var clickY = 0f
    private var clickInterval = 100L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d(TAG, "Accessibility Event: ${event?.eventType}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
        stopClicking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleCommand(it) }
        return START_STICKY
    }

    private fun handleCommand(intent: Intent) {
        when (intent.action) {
            ACTION_START_CLICKING -> {
                clickX = intent.getFloatExtra(EXTRA_X, 0f)
                clickY = intent.getFloatExtra(EXTRA_Y, 0f)
                startClicking()
            }
            ACTION_STOP_CLICKING -> {
                stopClicking()
            }
        }
    }

    private fun startClicking() {
        if (isClicking) return
        
        isClicking = true
        Log.d(TAG, "Clicking started at ($clickX, $clickY)")

        clickingJob = scope.launch {
            while (isClicking) {
                performClick(clickX, clickY)
                delay(clickInterval)
            }
        }
    }

    fun stopClicking() {
        isClicking = false
        clickingJob?.cancel()
        Log.d(TAG, "Clicking stopped")
    }

    private suspend fun performClick(x: Float, y: Float) {
        try {
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,
                        MIN_CLICK_INTERVAL
                    )
                )
                .build()

            var clickCompleted = false
            var clickFailed = false

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    clickCompleted = true
                    Log.d(TAG, "Click completed at ($x, $y)")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    clickFailed = true
                    Log.w(TAG, "Click cancelled at ($x, $y)")
                }
            }, null)

            withTimeoutOrNull(1000L) {
                while (!clickCompleted && !clickFailed) {
                    delay(10)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        stopClicking()
        super.onDestroy()
    }
}