package com.suilearn.feature.favorites

sealed interface FavoritesEvent {
    object Refresh : FavoritesEvent
}
