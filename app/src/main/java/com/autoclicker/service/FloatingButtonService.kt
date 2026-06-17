package com.autoclicker.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.autoclicker.R

class FloatingButtonService : Service() {

    companion object {
        private const val TAG = "FloatingButton"
    }

    private lateinit var windowManager: WindowManager
    private var floatingButton: ImageButton? = null
    private var params: WindowManager.LayoutParams? = null
    private var isPressed = false
    private var lastX = 0f
    private var lastY = 0f
    private var initialX = 0f
    private var initialY = 0f

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        floatingButton = ImageButton(this).apply {
            setBackgroundColor(android.graphics.Color.parseColor("#FF6200EE"))
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setImageDrawable(
                ContextCompat.getDrawable(this@FloatingButtonService, android.R.drawable.ic_dialog_info)
            )
        }

        // Android 11+ compatible WindowManager.LayoutParams
        params = WindowManager.LayoutParams().apply {
            // Always use TYPE_APPLICATION_OVERLAY for Android 8+
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            width = 100
            height = 100
            x = 200
            y = 200
        }

        try {
            windowManager.addView(floatingButton, params)
            setupFloatingButtonListeners()
            Log.d(TAG, "Floating button created successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating button", e)
        }
    }

    private fun setupFloatingButtonListeners() {
        floatingButton?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isPressed = true
                    initialX = event.rawX
                    initialY = event.rawY
                    lastX = event.rawX
                    lastY = event.rawY

                    // Make button touchable
                    params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    try {
                        windowManager.updateViewLayout(floatingButton, params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating layout on ACTION_DOWN", e)
                    }

                    startClickingAtButton()
                    Log.d(TAG, "Button pressed at (${event.rawX}, ${event.rawY})")
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    if (isPressed) {
                        val diffX = (event.rawX - lastX).toInt()
                        val diffY = (event.rawY - lastY).toInt()

                        params?.x = params?.x?.plus(diffX) ?: 0
                        params?.y = params?.y?.plus(diffY) ?: 0

                        try {
                            windowManager.updateViewLayout(floatingButton, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating layout on ACTION_MOVE", e)
                        }

                        lastX = event.rawX
                        lastY = event.rawY
                    }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    isPressed = false

                    stopClicking()

                    // Make button non-touchable again
                    params?.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    try {
                        windowManager.updateViewLayout(floatingButton, params)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error updating layout on ACTION_UP", e)
                    }

                    Log.d(TAG, "Button released")
                    true
                }

                else -> false
            }
        }
    }

    private fun startClickingAtButton() {
        val x = (params?.x ?: 0) + 50
        val y = (params?.y ?: 0) + 50

        val intent = Intent(this, AutoClickerAccessibilityService::class.java).apply {
            action = AutoClickerAccessibilityService.ACTION_START_CLICKING
            putExtra(AutoClickerAccessibilityService.EXTRA_X, x.toFloat())
            putExtra(AutoClickerAccessibilityService.EXTRA_Y, y.toFloat())
        }
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                @Suppress("DEPRECATION")
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting clicking service", e)
        }
    }

    private fun stopClicking() {
        val intent = Intent(this, AutoClickerAccessibilityService::class.java).apply {
            action = AutoClickerAccessibilityService.ACTION_STOP_CLICKING
        }
        try {
            startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping clicking service", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        try {
            floatingButton?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing floating button", e)
        }
        super.onDestroy()
        Log.d(TAG, "Floating button destroyed")
    }
}