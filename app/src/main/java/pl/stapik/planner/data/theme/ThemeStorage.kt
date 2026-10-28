package pl.stapik.planner.data.theme

import kotlinx.coroutines.flow.Flow
import pl.stapik.common.theme.ThemeId

interface ThemeStorage {
    val theme: Flow<ThemeId>
    suspend fun setTheme(theme: ThemeId)
}
