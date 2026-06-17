package com.autoclicker.viewmodel

import android.app.Application
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoclicker.repository.ClickerRepository
import com.autoclicker.repository.ClickerSettings
import com.autoclicker.service.FloatingButtonService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AutoClickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClickerRepository()
    val settings: StateFlow<ClickerSettings> = repository.settings

    fun startFloatingButton() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ctx.startForegroundService(intent)
        } else {
            ctx.startService(intent)
        }
    }

    fun stopFloatingButton() {
        val ctx = getApplication<Application>()
        ctx.stopService(Intent(ctx, FloatingButtonService::class.java))
    }

    fun updateClickInterval(interval: Long) {
        viewModelScope.launch { repository.setClickInterval(interval) }
        FloatingButtonService.clickIntervalMs = interval
    }
}
