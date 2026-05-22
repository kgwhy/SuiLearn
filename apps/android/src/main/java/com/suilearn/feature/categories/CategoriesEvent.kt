package com.suilearn.feature.categories

sealed interface CategoriesEvent {
    object Refresh : CategoriesEvent
}
