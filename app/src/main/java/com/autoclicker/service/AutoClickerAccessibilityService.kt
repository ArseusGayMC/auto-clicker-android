package com.autoclicker.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.*

class AutoClickerAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutoClicker"
        private const val MIN_CLICK_INTERVAL = 100L // Minimum click duration
        const val ACTION_START_CLICKING = "START_CLICKING"
        const val ACTION_STOP_CLICKING = "STOP_CLICKING"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
        const val EXTRA_INTERVAL = "extra_interval"
    }

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var clickingJob: Job? = null
    private var isClicking = false
    private var clickX = 0f
    private var clickY = 0f
    private var clickInterval = 100L
    private var consecutiveErrors = 0
    private val maxConsecutiveErrors = 5

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d(TAG, "Accessibility Event: ${event?.eventType}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Service Interrupted")
        stopClicking()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service Connected")
        Log.d(TAG, "Can Perform Gestures: ${serviceInfo?.canPerformGestures()}")
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
                clickInterval = intent.getLongExtra(EXTRA_INTERVAL, 100L)
                
                Log.d(TAG, "Start Command: x=$clickX, y=$clickY, interval=$clickInterval")
                
                if (clickX <= 0 || clickY <= 0) {
                    Log.e(TAG, "Invalid coordinates: x=$clickX, y=$clickY")
                    return
                }
                
                consecutiveErrors = 0
                startClicking()
            }
            ACTION_STOP_CLICKING -> {
                Log.d(TAG, "Stop Command received")
                stopClicking()
            }
        }
    }

    private fun startClicking() {
        if (isClicking) {
            Log.w(TAG, "Already clicking, ignoring start request")
            return
        }
        
        isClicking = true
        Log.d(TAG, "✅ Clicking started at ($clickX, $clickY) with interval $clickInterval ms")

        clickingJob = scope.launch {
            while (isClicking && consecutiveErrors < maxConsecutiveErrors) {
                try {
                    performClick(clickX, clickY)
                    delay(clickInterval)
                } catch (e: CancellationException) {
                    Log.d(TAG, "Clicking coroutine cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Error in clicking loop", e)
                    consecutiveErrors++
                    delay(100) // Small delay before retry
                }
            }
            
            if (consecutiveErrors >= maxConsecutiveErrors) {
                Log.e(TAG, "Too many consecutive errors, stopping")
                isClicking = false
            }
        }
    }

    fun stopClicking() {
        if (!isClicking) return
        
        isClicking = false
        clickingJob?.cancel()
        Log.d(TAG, "🛑 Clicking stopped after $consecutiveErrors errors")
    }

    private suspend fun performClick(x: Float, y: Float) {
        try {
            // Validate coordinates
            if (x <= 0 || y <= 0) {
                Log.e(TAG, "Invalid click coordinates: x=$x, y=$y")
                consecutiveErrors++
                return
            }

            // Create path with proper click gesture
            val path = Path().apply {
                moveTo(x, y)
                // Add a tiny line to ensure it's recognized as a stroke
                lineTo(x + 0.1f, y + 0.1f)
            }
            
            val gesture = GestureDescription.Builder()
                .addStroke(
                    GestureDescription.StrokeDescription(
                        path,
                        0,           // Start time
                        MIN_CLICK_INTERVAL  // Duration
                    )
                )
                .build()

            var clickCompleted = false
            var clickFailed = false
            var clickError: String? = null

            dispatchGesture(gesture, object : GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    clickCompleted = true
                    consecutiveErrors = 0
                    Log.v(TAG, "✓ Click completed at ($x, $y)")
                }

                override fun onCancelled(gestureDescription: GestureDescription?) {
                    clickFailed = true
                    clickError = "Gesture cancelled"
                    Log.w(TAG, "✗ Click cancelled at ($x, $y)")
                }
            }, null)

            // Wait for gesture to complete or timeout
            withTimeoutOrNull(1500L) {
                var waitCount = 0
                while (!clickCompleted && !clickFailed && waitCount < 150) {
                    delay(10)
                    waitCount++
                }
            }

            if (!clickCompleted && !clickFailed) {
                Log.w(TAG, "Click timeout at ($x, $y)")
                consecutiveErrors++
            } else if (clickFailed) {
                Log.w(TAG, "Click failed: $clickError at ($x, $y)")
                consecutiveErrors++
            }

        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: Accessibility Service might not have gesture permission", e)
            consecutiveErrors++
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click at ($x, $y)", e)
            consecutiveErrors++
        }
    }

    override fun onDestroy() {
        stopClicking()
        scope.cancel()
        Log.d(TAG, "Service destroyed")
        super.onDestroy()
    }
}