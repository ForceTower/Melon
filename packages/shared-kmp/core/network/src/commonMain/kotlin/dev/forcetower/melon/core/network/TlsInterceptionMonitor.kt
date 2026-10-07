package dev.forcetower.melon.core.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class TlsInterception(
    val host: String,
    val issuerName: String?,
)

// Remembers that the network re-signs the API's TLS (campus Fortinet, antivirus
// HTTPS inspection). Repositories collapse that into a generic transport error,
// so this is what lets Home explain why sync stopped. Any later response from
// the same host clears it.
class TlsInterceptionMonitor {
    private val state = MutableStateFlow<TlsInterception?>(null)
    val interception: StateFlow<TlsInterception?> = state.asStateFlow()

    internal fun report(
        host: String,
        issuerName: String?,
    ) {
        state.value = TlsInterception(host, issuerName)
    }

    internal fun clear(host: String) {
        state.update { current -> current?.takeUnless { it.host.equals(host, ignoreCase = true) } }
    }
}
