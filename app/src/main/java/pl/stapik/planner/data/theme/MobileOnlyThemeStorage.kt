package pl.stapik.planner.data.theme

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import pl.stapik.common.document.MobileOnlyStore
import pl.stapik.common.theme.ThemeId

class MobileOnlyThemeStorage(
    private val store: MobileOnlyStore<ThemePreference>,
    private val legacy: LegacyThemeSource
) : ThemeStorage {
    private val current = MutableStateFlow<ThemeId?>(null)

    override val theme: Flow<ThemeId> = current.onStart { ensureLoaded() }.filterNotNull()

    override suspend fun setTheme(theme: ThemeId) {
        store.save(ThemePreference(theme.id))
        current.value = theme
    }

    private suspend fun ensureLoaded() {
        if (current.value != null) return
        current.value = try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ThemeId.CLASSIC
        }
    }

    private suspend fun load(): ThemeId {
        store.load().themeId?.let { return ThemeId.fromId(it, default = ThemeId.CLASSIC) }
        val legacyName = legacy.read() ?: return ThemeId.CLASSIC
        val migrated = ThemeId.fromId(legacyName.lowercase().replace('_', '-'), default = ThemeId.CLASSIC)
        store.save(ThemePreference(migrated.id))
        legacy.clear()
        return migrated
    }
}
