package pl.stapik.planner.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import java.io.File
import java.time.Clock
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.crypto.KeystoreCiphers
import pl.stapik.common.document.FileDocumentStore
import pl.stapik.common.presentation.ConnectController
import pl.stapik.common.sync.SyncEngine
import pl.stapik.planner.data.cache.LegacyPlannerCache
import pl.stapik.planner.data.cloud.CloudGatewayProvider
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.config.ConnectionApiConfigStorage
import pl.stapik.planner.data.document.MigratingDocumentStore
import pl.stapik.planner.data.document.PlannerDocument
import pl.stapik.planner.data.localstate.DataStoreTodayChecksStorage
import pl.stapik.planner.data.model.PlannerPayload
import pl.stapik.planner.data.theme.DataStoreThemeStorage
import pl.stapik.planner.data.theme.ThemeStorage
import pl.stapik.planner.ui.plan.PlanViewModelFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()
    private val gatewayProvider = CloudGatewayProvider(connectionStorage, transport)

    private val documentStore = MigratingDocumentStore(
        delegate = FileDocumentStore(
            directory = File(appContext.filesDir, DOCUMENTS_DIRECTORY).toPath(),
            slot = PlannerDocument.slot,
            cipher = KeystoreCiphers.create(DOCUMENT_KEY_ALIAS, PlannerDocument.slot.toString().toByteArray())
        ),
        legacyCache = LegacyPlannerCache(appContext)
    )

    private val plannerEngine = SyncEngine<PlannerPayload>(
        slot = PlannerDocument.slot,
        codec = PlannerDocument.codec,
        store = documentStore,
        cloudProvider = { gatewayProvider.current() },
        clock = Clock.systemUTC()
    )

    val apiConfigStorage: ApiConfigStorage = connectionStorage

    val themeStorage: ThemeStorage = DataStoreThemeStorage(appContext)

    val planViewModelFactory: ViewModelProvider.Factory = PlanViewModelFactory(
        engine = plannerEngine,
        apiConfigStorage = connectionStorage,
        todayChecksStorage = DataStoreTodayChecksStorage(appContext)
    )

    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = listOf(plannerEngine),
            initial = initial
        )

    private companion object {
        const val DOCUMENTS_DIRECTORY = "stapik/documents"
        const val DOCUMENT_KEY_ALIAS = "stapikplanner_document_key"
    }
}
