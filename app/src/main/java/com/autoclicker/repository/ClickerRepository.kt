package com.autoclicker.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class ClickerSettings(
    val clickInterval: Long = 100L,
    val isEnabled: Boolean = false,
    val clickX: Float = 0f,
    val clickY: Float = 0f
)

class ClickerRepository {
    private val _settings = MutableStateFlow(ClickerSettings())
    val settings: StateFlow<ClickerSettings> = _settings

    fun updateSettings(settings: ClickerSettings) {
        _settings.value = settings
    }

    fun setClickInterval(interval: Long) {
        _settings.value = _settings.value.copy(clickInterval = interval)
    }

    fun setEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
    }
}