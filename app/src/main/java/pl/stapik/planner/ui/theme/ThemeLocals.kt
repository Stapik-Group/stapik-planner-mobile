package pl.stapik.planner.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import pl.stapik.common.ui.theme.StapikTheme

/**
 * The planner's view of the theme that [StapikTheme] sets, so the screens keep writing
 * `LocalThemeColors.current` and there is one source of truth for the current theme.
 */
object LocalThemeColors {
    val current: ThemeColors
        @Composable @ReadOnlyComposable get() = themeColorsFor(StapikTheme.colors.id)
}