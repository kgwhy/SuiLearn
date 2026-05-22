package com.suilearn.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suilearn.core.model.PracticeMode
import com.suilearn.feature.categories.CategoriesScreen
import com.suilearn.feature.categories.CategoriesViewModel
import com.suilearn.feature.favorites.FavoritesScreen
import com.suilearn.feature.favorites.FavoritesViewModel
import com.suilearn.feature.home.HomeScreen
import com.suilearn.feature.home.HomeViewModel
import com.suilearn.feature.knowledge.KnowledgePointScreen
import com.suilearn.feature.knowledge.KnowledgePointViewModel
import com.suilearn.feature.practice.PracticeScreen
import com.suilearn.feature.practice.PracticeEvent
import com.suilearn.feature.practice.PracticeViewModel
import com.suilearn.feature.search.SearchScreen
import com.suilearn.feature.search.SearchViewModel
import com.suilearn.feature.settings.SettingsScreen
import com.suilearn.feature.settings.SettingsViewModel
import com.suilearn.feature.statistics.StatisticsScreen
import com.suilearn.feature.statistics.StatisticsViewModel
import com.suilearn.feature.wrongbook.WrongBookScreen
import com.suilearn.feature.wrongbook.WrongBookViewModel
import com.suilearn.viewmodel.AppViewModelFactory

@Composable
fun SuiLearnNavHost(viewModelFactory: AppViewModelFactory) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val homeViewModel: HomeViewModel = viewModel(factory = viewModelFactory)
    val categoriesViewModel: CategoriesViewModel = viewModel(factory = viewModelFactory)
    val practiceViewModel: PracticeViewModel = viewModel(factory = viewModelFactory)
    val wrongBookViewModel: WrongBookViewModel = viewModel(factory = viewModelFactory)
    val favoritesViewModel: FavoritesViewModel = viewModel(factory = viewModelFactory)
    val searchViewModel: SearchViewModel = viewModel(factory = viewModelFactory)
    val knowledgePointViewModel: KnowledgePointViewModel = viewModel(factory = viewModelFactory)
    val statisticsViewModel: StatisticsViewModel = viewModel(factory = viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
    val bottomBarRoutes = setOf(
        AppDestination.Home.route,
        AppDestination.Categories.route,
        AppDestination.WrongBook.route,
        AppDestination.Favorites.route,
        AppDestination.Search.route,
        AppDestination.Statistics.route,
        AppDestination.Settings.route,
    )

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes || currentRoute == null) {
                NavigationBar {
                    bottomItems.forEach { destination ->
                        val selected = currentRoute == destination.route
                        val icon = when (destination) {
                            AppDestination.Home -> Icons.Outlined.Home
                            AppDestination.Categories -> Icons.Outlined.Category
                            AppDestination.WrongBook -> Icons.Outlined.ErrorOutline
                            AppDestination.Favorites -> Icons.Outlined.StarBorder
                            AppDestination.Search -> Icons.Outlined.Search
                            AppDestination.Statistics -> Icons.Outlined.BarChart
                            AppDestination.Settings -> Icons.Outlined.Settings
                            AppDestination.Practice, AppDestination.Knowledge -> Icons.Outlined.Home
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = destination.route) },
                            label = { Text(bottomLabel(destination)) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = AppDestination.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(AppDestination.Home.route) {
                HomeScreen(
                    homeViewModel = homeViewModel,
                    onStartPractice = { mode, targetId ->
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(mode, targetId))
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onResumePractice = {
                        practiceViewModel.onEvent(PracticeEvent.Resume())
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onOpenKnowledgePoint = { navController.navigate("${AppDestination.Knowledge.route}/$it") },
                    onOpenSearch = { navController.navigate(AppDestination.Search.route) },
                    onOpenStatistics = { navController.navigate(AppDestination.Statistics.route) },
                    onOpenFavorites = { navController.navigate(AppDestination.Favorites.route) },
                    onOpenWrongBook = { navController.navigate(AppDestination.WrongBook.route) },
                )
            }
            composable(AppDestination.Practice.route) {
                PracticeScreen(
                    practiceViewModel = practiceViewModel,
                    onFinish = { navController.popBackStack() },
                )
            }
            composable(AppDestination.Categories.route) {
                CategoriesScreen(
                    categoriesViewModel = categoriesViewModel,
                    onStartCategory = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.CATEGORY, it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.WrongBook.route) {
                WrongBookScreen(
                    wrongBookViewModel = wrongBookViewModel,
                    onStartWrongPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.WRONG_QUESTION))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.Favorites.route) {
                FavoritesScreen(
                    favoritesViewModel = favoritesViewModel,
                    onStartFavoritePractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.FAVORITE))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.Search.route) {
                SearchScreen(
                    searchViewModel = searchViewModel,
                    onOpenKnowledgePoint = { navController.navigate("${AppDestination.Knowledge.route}/$it") },
                    onStartPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartFromQuestion(it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable("${AppDestination.Knowledge.route}/{id}") { entry ->
                KnowledgePointScreen(
                    knowledgePointViewModel = knowledgePointViewModel,
                    knowledgePointId = entry.arguments?.getString("id").orEmpty(),
                    onStartPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.KNOWLEDGE_POINT, it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.Statistics.route) {
                StatisticsScreen(statisticsViewModel = statisticsViewModel)
            }
            composable(AppDestination.Settings.route) {
                SettingsScreen(settingsViewModel = settingsViewModel)
            }
        }
    }
}

private val bottomItems = listOf(
    AppDestination.Home,
    AppDestination.Categories,
    AppDestination.WrongBook,
    AppDestination.Favorites,
    AppDestination.Search,
    AppDestination.Statistics,
    AppDestination.Settings,
)

private fun bottomLabel(destination: AppDestination): String = when (destination) {
    AppDestination.Home -> "首页"
    AppDestination.Categories -> "分类"
    AppDestination.WrongBook -> "错题"
    AppDestination.Favorites -> "收藏"
    AppDestination.Search -> "搜索"
    AppDestination.Statistics -> "统计"
    AppDestination.Settings -> "设置"
    AppDestination.Practice -> "练习"
    AppDestination.Knowledge -> "知识点"
}
