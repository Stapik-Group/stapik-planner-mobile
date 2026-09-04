package pl.stapik.planner.ui.navigation

sealed interface AppScreen {
    data object Plan: AppScreen
    data object Connect: AppScreen
    data object About: AppScreen
    data object Theme: AppScreen
}
