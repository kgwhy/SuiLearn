package com.suilearn.navigation

sealed class AppDestination(val route: String) {
    data object Home : AppDestination("home")
    data object Practice : AppDestination("practice")
    data object Categories : AppDestination("categories")
    data object WrongBook : AppDestination("wrongbook")
    data object Favorites : AppDestination("favorites")
    data object Search : AppDestination("search")
    data object Knowledge : AppDestination("knowledge")
    data object Statistics : AppDestination("statistics")
    data object Settings : AppDestination("settings")
}
