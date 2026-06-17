package com.autoclicker.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ClickerSettings(
    val clickInterval: Long = 100L,
    val isEnabled: Boolean = false
)

class ClickerRepository {
    private val _settings = MutableStateFlow(ClickerSettings())
    val settings: StateFlow<ClickerSettings> = _settings.asStateFlow()

    fun setClickInterval(interval: Long) {
        _settings.value = _settings.value.copy(clickInterval = interval)
    }

    fun setEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(isEnabled = enabled)
    }
}
