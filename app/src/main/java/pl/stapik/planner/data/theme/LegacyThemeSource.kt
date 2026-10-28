package pl.stapik.planner.data.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.themeDataStore by preferencesDataStore(name = "app_theme")

class LegacyThemeSource(private val context: Context) {
    suspend fun read(): String? = context.themeDataStore.data.first()[KEY_THEME]

    suspend fun clear() {
        context.themeDataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("selected_theme")
    }
}
