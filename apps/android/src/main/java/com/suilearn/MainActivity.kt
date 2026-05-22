package com.suilearn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suilearn.navigation.SuiLearnNavHost
import com.suilearn.theme.SuiLearnTheme
import com.suilearn.viewmodel.AppViewModel
import com.suilearn.viewmodel.AppViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SuiLearnApp
        setContent {
            SuiLearnTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val viewModelFactory = AppViewModelFactory(app.appDependencies)
                    viewModel<AppViewModel>(
                        factory = viewModelFactory,
                    )
                    SuiLearnNavHost(viewModelFactory = viewModelFactory)
                }
            }
        }
    }
}
