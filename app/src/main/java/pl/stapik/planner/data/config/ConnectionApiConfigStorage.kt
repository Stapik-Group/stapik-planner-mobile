package pl.stapik.planner.data.config

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.crypto.KeystoreKeys
import pl.stapik.common.settings.AesGcmLegacyDecryptor
import pl.stapik.common.settings.ConfigLoad
import pl.stapik.common.settings.ConnectionConfigStore
import pl.stapik.common.settings.LegacyFormat
import pl.stapik.common.settings.LegacyPrefsConfigReader
import pl.stapik.common.settings.LegacyPrefsSource

/** The DataStore of the pre-library versions; now only read once, to migrate the connection. */
private val Context.apiConfigDataStore by preferencesDataStore(name = "api_config")

private class DataStoreLegacySource(private val dataStore: DataStore<Preferences>) : LegacyPrefsSource {
    override fun readAll(): Map<String, String> = runBlocking {
        dataStore.data.first().asMap().entries
            .mapNotNull { (key, value) -> (value as? String)?.let { key.name to it } }
            .toMap()
    }

    override fun clear() = runBlocking { dataStore.edit { it.clear() }; Unit }
}

/**
 * Connection settings in the library's encrypted file. The first [load] without that file migrates
 * the configuration saved by earlier versions, so the user does not connect again.
 */
class ConnectionApiConfigStorage(context: Context) : ApiConfigStorage {
    /** Kept for the connect form's controller, so that it can work on the same file. */
    val store = ConnectionConfigStore(
        directory = File(context.applicationContext.filesDir, "stapik").toPath(),
        cipher = KeystoreCiphers.create(KEY_ALIAS),
        schemaVersion = SCHEMA_VERSION,
        legacy = LegacyPrefsConfigReader(
            source = DataStoreLegacySource(context.applicationContext.apiConfigDataStore),
            format = LegacyFormat.CALENDAR_PLANNER,
            decryptor = AesGcmLegacyDecryptor { KeystoreKeys.find(LEGACY_KEY_ALIAS) },
        ),
    )

    override suspend fun load(): ApiConfig? = locked {
        when (val result = store.load()) {
            is ConfigLoad.Loaded -> ApiConfig(result.config.normalizedApiUrl, result.config.apiKey, result.config.passphrase)
            ConfigLoad.None -> null
            else -> {
                Log.w(TAG, "Saved connection is not usable: ${result::class.simpleName}")
                null
            }
        }
    }

    override suspend fun save(config: ApiConfig) = locked {
        store.save(CloudConfig(config.baseUrl, config.apiKey, config.passphrase))
    }

    override suspend fun clear() = locked { store.clear() }

    private suspend fun <T> locked(block: () -> T): T = lock.withLock { withContext(Dispatchers.IO) { block() } }

    private companion object {
        const val TAG = "ConnectionApiConfigStorage"
        const val SCHEMA_VERSION = 1
        const val KEY_ALIAS = "stapikplanner_connection_key"
        const val LEGACY_KEY_ALIAS = "stapikplanner_api_config_key"

        // ConnectionConfigStore is not thread-safe. The lock is shared by all instances, because
        // a view model that survives a rotation keeps the instance it was created with, while the
        // new activity creates another one for the same file.
        val lock = Mutex()
    }
}
