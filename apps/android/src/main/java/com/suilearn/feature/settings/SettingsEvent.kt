package com.suilearn.feature.settings

sealed interface SettingsEvent {
    object Refresh : SettingsEvent
    object ResetLocalData : SettingsEvent
}
