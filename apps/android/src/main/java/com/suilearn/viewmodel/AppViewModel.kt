package com.suilearn.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.di.AppDependencies
import kotlinx.coroutines.launch

class AppViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    init {
        viewModelScope.launch {
            dependencies.ensureSeeded()
        }
    }
}
