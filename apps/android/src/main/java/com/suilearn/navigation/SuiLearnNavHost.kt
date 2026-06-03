package com.suilearn.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.suilearn.core.model.PracticeMode
import com.suilearn.feature.ai.AiKnowledgeEntryScreen
import com.suilearn.feature.ai.AiKnowledgeViewModel
import com.suilearn.feature.categories.CategoriesScreen
import com.suilearn.feature.categories.CategoriesViewModel
import com.suilearn.feature.favorites.FavoritesScreen
import com.suilearn.feature.favorites.FavoritesViewModel
import com.suilearn.feature.home.HomeScreen
import com.suilearn.feature.home.HomeViewModel
import com.suilearn.feature.knowledge.KnowledgeMapScreen
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
    val aiKnowledgeViewModel: AiKnowledgeViewModel = viewModel(factory = viewModelFactory)
    val bottomBarRoutes = setOf(
        AppDestination.Home.route,
        AppDestination.AiKnowledge.route,
        AppDestination.Library.route,
        AppDestination.Review.route,
        AppDestination.Profile.route,
        AppDestination.Categories.route,
        AppDestination.WrongBook.route,
        AppDestination.Favorites.route,
        AppDestination.Search.route,
        AppDestination.Knowledge.route,
        "${AppDestination.Knowledge.route}/{id}",
        AppDestination.Statistics.route,
        AppDestination.Settings.route,
    )
    val selectedBottomDestination = selectedBottomDestination(currentRoute)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in bottomBarRoutes || currentRoute == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    bottomItems.forEach { destination ->
                        val selected = selectedBottomDestination == destination
                        val icon = when (destination) {
                            AppDestination.Home -> Icons.Outlined.Home
                            AppDestination.AiKnowledge -> Icons.Outlined.AutoAwesome
                            AppDestination.Categories -> Icons.Outlined.Category
                            AppDestination.WrongBook -> Icons.Outlined.ErrorOutline
                            AppDestination.Favorites -> Icons.Outlined.StarBorder
                            AppDestination.Search -> Icons.Outlined.Search
                            AppDestination.Knowledge -> Icons.Outlined.Category
                            AppDestination.Statistics -> Icons.Outlined.BarChart
                            AppDestination.Settings -> Icons.Outlined.Settings
                            AppDestination.Library -> Icons.Outlined.Category
                            AppDestination.Review -> Icons.Outlined.BarChart
                            AppDestination.Profile -> Icons.Outlined.Person
                            AppDestination.Practice -> Icons.Outlined.Home
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigateToBottomDestination(destination, currentRoute)
                            },
                            icon = { Icon(icon, contentDescription = destination.route) },
                            label = { Text(bottomLabel(destination)) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
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
                    onOpenAiKnowledge = { navController.navigate(AppDestination.AiKnowledge.route) },
                )
            }
            composable(AppDestination.AiKnowledge.route) {
                AiKnowledgeEntryScreen(
                    aiKnowledgeViewModel = aiKnowledgeViewModel,
                    onStartLocalPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.SEQUENTIAL, null))
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onOpenKnowledgeMap = { navController.navigate(AppDestination.Knowledge.route) },
                )
            }
            composable(AppDestination.Practice.route) {
                PracticeScreen(
                    practiceViewModel = practiceViewModel,
                    onFinish = { navController.popBackStack() },
                )
            }
            composable(AppDestination.Library.route) {
                LibraryHubScreen(
                    categoriesViewModel = categoriesViewModel,
                    knowledgePointViewModel = knowledgePointViewModel,
                    onStartCategory = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.CATEGORY, it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onOpenKnowledgePoint = { navController.navigate("${AppDestination.Knowledge.route}/$it") },
                    onStartKnowledgePractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.KNOWLEDGE_POINT, it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.Review.route) {
                ReviewHubScreen(
                    wrongBookViewModel = wrongBookViewModel,
                    favoritesViewModel = favoritesViewModel,
                    statisticsViewModel = statisticsViewModel,
                    onStartWrongPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.WRONG_QUESTION))
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onStartFavoritePractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.FAVORITE))
                        navController.navigate(AppDestination.Practice.route)
                    },
                    onStartQuestionPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartFromQuestion(it))
                        navController.navigate(AppDestination.Practice.route)
                    },
                )
            }
            composable(AppDestination.Profile.route) {
                SettingsScreen(settingsViewModel = settingsViewModel)
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
                    onStartQuestionPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartFromQuestion(it))
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
            composable(AppDestination.Knowledge.route) {
                KnowledgeMapScreen(
                    knowledgePointViewModel = knowledgePointViewModel,
                    onOpenKnowledgePoint = { navController.navigate("${AppDestination.Knowledge.route}/$it") },
                    onStartPractice = {
                        practiceViewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.KNOWLEDGE_POINT, it))
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
    AppDestination.Library,
    AppDestination.Review,
    AppDestination.Profile,
)

private val primaryBottomRoutes = bottomItems.map { it.route }.toSet()

private fun NavHostController.navigateToBottomDestination(
    destination: AppDestination,
    currentRoute: String?,
) {
    val shouldRestoreState = currentRoute in primaryBottomRoutes && destination != AppDestination.Home

    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = shouldRestoreState
        }
        launchSingleTop = true
        restoreState = shouldRestoreState
    }
}

private fun bottomLabel(destination: AppDestination): String = when (destination) {
    AppDestination.Home -> "首页"
    AppDestination.AiKnowledge -> "AI"
    AppDestination.Library -> "题库"
    AppDestination.Review -> "复盘"
    AppDestination.Profile -> "我的"
    AppDestination.Categories -> "分类"
    AppDestination.WrongBook -> "错题"
    AppDestination.Favorites -> "收藏"
    AppDestination.Search -> "搜索"
    AppDestination.Statistics -> "统计"
    AppDestination.Settings -> "设置"
    AppDestination.Practice -> "练习"
    AppDestination.Knowledge -> "知识点"
}

private fun selectedBottomDestination(currentRoute: String?): AppDestination? = when {
    currentRoute == null -> AppDestination.Home
    currentRoute == AppDestination.Home.route ||
        currentRoute == AppDestination.Search.route ||
        currentRoute == AppDestination.AiKnowledge.route -> AppDestination.Home
    currentRoute == AppDestination.Library.route ||
        currentRoute == AppDestination.Categories.route ||
        currentRoute.startsWith(AppDestination.Knowledge.route) -> AppDestination.Library
    currentRoute == AppDestination.Review.route ||
        currentRoute == AppDestination.WrongBook.route ||
        currentRoute == AppDestination.Favorites.route ||
        currentRoute == AppDestination.Statistics.route -> AppDestination.Review
    currentRoute == AppDestination.Profile.route || currentRoute == AppDestination.Settings.route -> AppDestination.Profile
    else -> null
}

@Composable
private fun LibraryHubScreen(
    categoriesViewModel: CategoriesViewModel,
    knowledgePointViewModel: KnowledgePointViewModel,
    onStartCategory: (String) -> Unit,
    onOpenKnowledgePoint: (String) -> Unit,
    onStartKnowledgePractice: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("分类", "知识点")

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }
        when (selectedTab) {
            0 -> CategoriesScreen(
                categoriesViewModel = categoriesViewModel,
                onStartCategory = onStartCategory,
            )
            1 -> KnowledgeMapScreen(
                knowledgePointViewModel = knowledgePointViewModel,
                onOpenKnowledgePoint = onOpenKnowledgePoint,
                onStartPractice = onStartKnowledgePractice,
            )
        }
    }
}

@Composable
private fun ReviewHubScreen(
    wrongBookViewModel: WrongBookViewModel,
    favoritesViewModel: FavoritesViewModel,
    statisticsViewModel: StatisticsViewModel,
    onStartWrongPractice: () -> Unit,
    onStartFavoritePractice: () -> Unit,
    onStartQuestionPractice: (String) -> Unit,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("错题", "收藏", "统计")

    Column(Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
        ) {
            tabs.forEachIndexed { index, label ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(label) },
                )
            }
        }
        when (selectedTab) {
            0 -> WrongBookScreen(
                wrongBookViewModel = wrongBookViewModel,
                onStartWrongPractice = onStartWrongPractice,
            )
            1 -> FavoritesScreen(
                favoritesViewModel = favoritesViewModel,
                onStartFavoritePractice = onStartFavoritePractice,
                onStartQuestionPractice = onStartQuestionPractice,
            )
            2 -> StatisticsScreen(statisticsViewModel = statisticsViewModel)
        }
    }
}
