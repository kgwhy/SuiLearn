package com.suilearn.feature.categories

import com.suilearn.core.model.Category

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
)

