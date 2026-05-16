package dev.forcetower.melon.core.sync.domain.model

sealed class SyncError {
    data object NoConnection : SyncError()
    data object Unauthorized : SyncError()
    data object NotFound : SyncError()
    data class Server(val message: String?) : SyncError()
    data object Unexpected : SyncError()
}
