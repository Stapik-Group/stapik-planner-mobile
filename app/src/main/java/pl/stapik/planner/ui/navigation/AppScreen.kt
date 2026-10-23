package pl.stapik.planner.ui.navigation

sealed interface AppScreen {
    data object Plan: AppScreen
    data object Settings: AppScreen
}
