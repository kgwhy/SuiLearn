package com.suilearn.feature.favorites

sealed interface FavoritesEvent {
    object Refresh : FavoritesEvent

    data class ToggleFavorite(
        val questionId: String,
    ) : FavoritesEvent
}
