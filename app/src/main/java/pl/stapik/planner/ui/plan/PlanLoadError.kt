package pl.stapik.planner.ui.plan

import pl.stapik.common.sync.CloudError
import pl.stapik.common.sync.CloudException
import pl.stapik.common.sync.UnreadableKind
import pl.stapik.planner.data.repository.PlannerNotFoundException

sealed interface PlanLoadError {
    data object NoNetwork : PlanLoadError
    data object Unauthorized : PlanLoadError
    data object NotFound : PlanLoadError

    /** The cloud data cannot be read (encrypted, wrong passphrase, newer format); [messageKey] is a library message key. */
    data class Unreadable(val messageKey: String) : PlanLoadError

    data class Unknown(val message: String) : PlanLoadError
}

fun Throwable.toPlanLoadError(): PlanLoadError = when (this) {
    is PlannerNotFoundException -> PlanLoadError.NotFound
    is CloudException -> when (val cloud = error) {
        CloudError.Unauthorized, CloudError.Forbidden -> PlanLoadError.Unauthorized
        is CloudError.Network -> PlanLoadError.NoNetwork
        is CloudError.Unreadable -> PlanLoadError.Unreadable(unreadableKey(cloud.kind))
        is CloudError.ServerError -> PlanLoadError.Unknown("HTTP ${cloud.httpCode}")
        is CloudError.RateLimited -> PlanLoadError.Unknown("HTTP 429")
        CloudError.Conflict -> PlanLoadError.Unknown("Conflict")
    }
    else -> PlanLoadError.Unknown(message ?: this::class.simpleName ?: "Unknown error")
}

private fun unreadableKey(kind: UnreadableKind): String = when (kind) {
    UnreadableKind.PASSPHRASE_REQUIRED -> "sync.unreadable.passphrase_required"
    UnreadableKind.DECRYPTION_FAILED -> "sync.unreadable.wrong_passphrase"
    UnreadableKind.UNSUPPORTED_VERSION -> "sync.unreadable.newer_version"
    UnreadableKind.OTHER -> "sync.unreadable.other"
}
