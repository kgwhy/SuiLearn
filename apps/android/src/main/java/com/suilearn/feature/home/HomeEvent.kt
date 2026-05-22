package com.suilearn.feature.home

sealed interface HomeEvent {
    object Refresh : HomeEvent
}
