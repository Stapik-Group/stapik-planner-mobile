package pl.stapik.planner.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.stapik.planner.R
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.theme.ThemeStorage
import pl.stapik.planner.ui.about.AboutScreen
import pl.stapik.planner.ui.connect.ConnectScreen
import pl.stapik.planner.ui.navigation.AppScreen
import pl.stapik.planner.ui.plan.PlanPagerScreen
import pl.stapik.planner.ui.theme.AppTheme
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.ThemePalettes
import pl.stapik.planner.ui.theme.ThemeScreen

@Composable
fun AppRoot(
    apiConfigStorage: ApiConfigStorage,
    themeStorage: ThemeStorage,
    modifier: Modifier = Modifier
) {
    val currentTheme by themeStorage.theme.collectAsState(initial = AppTheme.CLASSIC)

    CompositionLocalProvider(
        LocalThemeColors provides ThemePalettes.forTheme(currentTheme)
    ) {
        val themeColors = LocalThemeColors.current
        var screen by remember { mutableStateOf<AppScreen>(AppScreen.Plan) }
        var menuExpanded by remember { mutableStateOf(false) }
        BackHandler(enabled = screen != AppScreen.Plan) {
            screen = AppScreen.Plan
        }
        Scaffold(
            modifier = modifier,
            containerColor = themeColors.windowBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (screen == AppScreen.Plan) {
                    Box(modifier = Modifier.navigationBarsPadding()) {
                        FloatingActionButton(
                            onClick = { menuExpanded = true },
                            containerColor = themeColors.cellBackground,
                            contentColor = themeColors.textDark
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_connect)) },
                                onClick = { menuExpanded = false; screen = AppScreen.Connect }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_about)) },
                                onClick = { menuExpanded = false; screen = AppScreen.About }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.menu_theme)) },
                                onClick = { menuExpanded = false; screen = AppScreen.Theme }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (screen) {
                    AppScreen.Plan -> PlanPagerScreen(
                        apiConfigStorage = apiConfigStorage,
                        onNavigateToConnect = { screen = AppScreen.Connect }
                    )
                    AppScreen.Connect -> ConnectScreen(storage = apiConfigStorage, onBack = { screen = AppScreen.Plan })
                    AppScreen.About -> AboutScreen(onBack = { screen = AppScreen.Plan })
                    AppScreen.Theme -> ThemeScreen(storage = themeStorage, onBack = { screen = AppScreen.Plan })
                }
            }
        }
    }
}
