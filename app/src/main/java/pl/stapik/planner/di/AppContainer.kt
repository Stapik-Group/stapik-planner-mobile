package pl.stapik.planner.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.OkHttpTransport
import pl.stapik.common.presentation.ConnectController
import pl.stapik.planner.data.cache.DataStorePlannerCacheStorage
import pl.stapik.planner.data.config.ApiConfigStorage
import pl.stapik.planner.data.config.ConnectionApiConfigStorage
import pl.stapik.planner.data.localstate.DataStoreTodayChecksStorage
import pl.stapik.planner.data.repository.PlannerRepository
import pl.stapik.planner.data.theme.DataStoreThemeStorage
import pl.stapik.planner.data.theme.ThemeStorage
import pl.stapik.planner.ui.plan.PlanViewModelFactory

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    private val connectionStorage = ConnectionApiConfigStorage(appContext)
    private val transport = OkHttpTransport()

    val apiConfigStorage: ApiConfigStorage = connectionStorage

    val themeStorage: ThemeStorage = DataStoreThemeStorage(appContext)

    private val plannerRepository = PlannerRepository(
        apiConfigStorage = connectionStorage,
        cacheStorage = DataStorePlannerCacheStorage(appContext),
        transport = transport
    )

    val planViewModelFactory: ViewModelProvider.Factory = PlanViewModelFactory(
        repository = plannerRepository,
        todayChecksStorage = DataStoreTodayChecksStorage(appContext)
    )

    fun newConnectController(initial: CloudConfig?): ConnectController =
        ConnectController(
            store = connectionStorage.store,
            gatewayFactory = { config -> HttpCloudGateway(config, transport) },
            documents = emptyList(),
            initial = initial
        )
}
