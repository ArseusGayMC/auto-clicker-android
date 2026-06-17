package com.autoclicker.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.autoclicker.repository.ClickerRepository
import com.autoclicker.repository.ClickerSettings
import com.autoclicker.service.AutoClickerAccessibilityService
import com.autoclicker.service.FloatingButtonService
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AutoClickerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ClickerRepository()
    val settings: StateFlow<ClickerSettings> = repository.settings

    fun startFloatingButton() {
        val context = getApplication<Application>()
        Intent(context, FloatingButtonService::class.java).apply {
            context.startService(this)
        }
    }

    fun stopFloatingButton() {
        val context = getApplication<Application>()
        Intent(context, FloatingButtonService::class.java).apply {
            context.stopService(this)
        }
    }

    fun updateClickInterval(interval: Long) {
        viewModelScope.launch {
            repository.setClickInterval(interval)
        }
    }

    fun enableClicking(enabled: Boolean) {
        viewModelScope.launch {
            repository.setEnabled(enabled)
        }
    }
}