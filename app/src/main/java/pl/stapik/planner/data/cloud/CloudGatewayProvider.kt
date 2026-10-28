package pl.stapik.planner.data.cloud

import pl.stapik.common.cloud.CloudConfig
import pl.stapik.common.cloud.HttpCloudGateway
import pl.stapik.common.cloud.HttpTransport
import pl.stapik.common.sync.CloudGateway
import pl.stapik.planner.data.config.ApiConfig
import pl.stapik.planner.data.config.ApiConfigStorage

class CloudGatewayProvider(
    private val configStorage: ApiConfigStorage,
    private val transport: HttpTransport
) {
    private var cached: Pair<ApiConfig, CloudGateway>? = null

    suspend fun current(): CloudGateway? = configStorage.load()?.let(::gatewayFor)

    @Synchronized
    private fun gatewayFor(config: ApiConfig): CloudGateway {
        cached?.let { (known, ready) -> if (known == config) return ready }
        val created = ReadOnlyCloudGateway(
            HttpCloudGateway(CloudConfig(config.baseUrl, config.apiKey, config.passphrase), transport)
        )
        cached = config to created
        return created
    }
}
