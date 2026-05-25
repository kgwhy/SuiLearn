package com.suilearn.feature.categories

import com.suilearn.core.model.CategoryProgress

data class CategoriesUiState(
    val categories: List<CategoryProgress> = emptyList(),
)
