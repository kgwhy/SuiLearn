package com.suilearn.feature.search

sealed interface SearchEvent {
    object Refresh : SearchEvent

    data class QueryChanged(
        val query: String,
    ) : SearchEvent
}
