package com.suilearn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.suilearn.di.AppDependencies
import com.suilearn.feature.categories.CategoriesViewModel
import com.suilearn.feature.favorites.FavoritesViewModel
import com.suilearn.feature.home.HomeViewModel
import com.suilearn.feature.knowledge.KnowledgePointViewModel
import com.suilearn.feature.practice.PracticeViewModel
import com.suilearn.feature.search.SearchViewModel
import com.suilearn.feature.settings.SettingsViewModel
import com.suilearn.feature.statistics.StatisticsViewModel
import com.suilearn.feature.wrongbook.WrongBookViewModel

class AppViewModelFactory(
    private val dependencies: AppDependencies,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel = when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(dependencies)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(dependencies)
            modelClass.isAssignableFrom(CategoriesViewModel::class.java) -> CategoriesViewModel(dependencies)
            modelClass.isAssignableFrom(PracticeViewModel::class.java) -> PracticeViewModel(dependencies)
            modelClass.isAssignableFrom(WrongBookViewModel::class.java) -> WrongBookViewModel(dependencies)
            modelClass.isAssignableFrom(FavoritesViewModel::class.java) -> FavoritesViewModel(dependencies)
            modelClass.isAssignableFrom(SearchViewModel::class.java) -> SearchViewModel(dependencies)
            modelClass.isAssignableFrom(KnowledgePointViewModel::class.java) -> KnowledgePointViewModel(dependencies)
            modelClass.isAssignableFrom(StatisticsViewModel::class.java) -> StatisticsViewModel(dependencies)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(dependencies)
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return viewModel as T
    }
}
