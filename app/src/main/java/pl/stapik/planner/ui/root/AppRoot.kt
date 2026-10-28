package pl.stapik.planner.ui.root

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.theme.ThemeId
import pl.stapik.common.ui.components.StapikButton
import pl.stapik.common.ui.shell.AppDefinition
import pl.stapik.common.ui.shell.SettingsFlow
import pl.stapik.common.ui.theme.StapikTheme
import pl.stapik.planner.BuildConfig
import pl.stapik.planner.R
import pl.stapik.planner.data.config.ApiConfig
import pl.stapik.planner.di.AppContainer
import pl.stapik.planner.ui.activities.ActivitiesScreen
import pl.stapik.planner.ui.navigation.AppScreen
import pl.stapik.planner.ui.plan.PlanPagerScreen
import pl.stapik.planner.ui.theme.LocalThemeColors
import pl.stapik.planner.ui.theme.SystemBarsAppearance
import pl.stapik.planner.util.AppInfo

private val appDefinition = AppDefinition(
    appId = "planner",
    name = "Stapik Planner",
    versionName = BuildConfig.VERSION_NAME,
    author = AppInfo.AUTHOR,
    websiteUrl = AppInfo.GITHUB_URL
)

@Composable
fun AppRoot(
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val apiConfigStorage = container.apiConfigStorage
    val themeStorage = container.themeStorage
    val currentTheme by themeStorage.theme.collectAsState(initial = ThemeId.CLASSIC)

    StapikTheme(currentTheme) {
        SystemBarsAppearance(darkTheme = StapikTheme.colors.isDark)
        val themeColors = LocalThemeColors.current
        var screen by remember { mutableStateOf<AppScreen>(AppScreen.Plan) }
        var savedConfig by remember { mutableStateOf<ApiConfig?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) { savedConfig = apiConfigStorage.load() }

        // The back button is handled by the settings themselves (a page back to the list, the list back to the plan).
        Scaffold(
            modifier = modifier,
            containerColor = themeColors.windowBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            floatingActionButton = {
                if (screen == AppScreen.Plan) {
                    FloatingActionButton(
                        onClick = { screen = AppScreen.Settings },
                        modifier = Modifier.navigationBarsPadding(),
                        containerColor = themeColors.cellBackground,
                        contentColor = themeColors.textDark
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings))
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (screen) {
                    AppScreen.Plan -> PlanPagerScreen(
                        viewModelFactory = container.planViewModelFactory,
                        onNavigateToConnect = { screen = AppScreen.Settings }
                    )
                    AppScreen.Settings -> {
                        val connectController = remember {
                            container.newConnectController(savedConfig?.let { CloudConfig(it.baseUrl, it.apiKey, it.passphrase) })
                        }
                        // The shared screens draw their own header under the status bar; the navigation bar
                        // and the keyboard are left to this padding.
                        SettingsFlow(
                            app = appDefinition,
                            themeId = currentTheme,
                            onThemeChange = { theme -> scope.launch { themeStorage.setTheme(theme) } },
                            connectController = connectController,
                            connected = savedConfig != null,
                            onConnected = { scope.launch { savedConfig = apiConfigStorage.load() } },
                            onDisconnected = { savedConfig = null },
                            onClose = { screen = AppScreen.Plan },
                            modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding(),
                            extra = {
                                StapikButton(
                                    text = stringResource(R.string.activities_title),
                                    onClick = { screen = AppScreen.Activities },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        )
                    }
                    AppScreen.Activities -> ActivitiesScreen(
                        viewModelFactory = container.planViewModelFactory,
                        onBack = { screen = AppScreen.Settings },
                        modifier = Modifier.fillMaxSize().navigationBarsPadding()
                    )
                }
            }
        }
    }
}
