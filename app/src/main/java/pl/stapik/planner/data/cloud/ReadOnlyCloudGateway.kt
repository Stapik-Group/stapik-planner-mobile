package pl.stapik.planner.data.cloud

import pl.stapik.common.model.CloudInfo
import pl.stapik.common.model.CloudScope
import pl.stapik.common.model.SlotKey
import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.CloudGateway
import pl.stapik.common.sync.PartitionInfo
import pl.stapik.common.sync.RemoteDocument

class ReadOnlyCloudGateway(private val delegate: CloudGateway) : CloudGateway {
    override suspend fun info(): CloudInfo = delegate.info().copy(scope = CloudScope.READ_ONLY)

    override suspend fun read(slot: SlotKey): RemoteDocument? = delegate.read(slot)

    override suspend fun write(slot: SlotKey, content: String, baseline: String): RemoteDocument =
        throw CloudException(CloudError.Forbidden)

    override suspend fun partitions(slot: SlotKey): List<PartitionInfo> = delegate.partitions(slot)
}
