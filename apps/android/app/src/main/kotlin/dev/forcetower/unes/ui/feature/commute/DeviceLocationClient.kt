package dev.forcetower.unes.ui.feature.commute

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.forcetower.melon.feature.transit.domain.model.GeoPoint
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

// While-in-use location, only ever asked for from a button the student taps
// (iOS `LocationClient`). The screen owns the permission prompt; this only
// reads the answer and waits for one fix. Nothing here is stored or sent to
// the backend.
@Singleton
internal class DeviceLocationClient @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val permissions: Array<String> =
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

    fun isGranted(): Boolean =
        permissions.any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    // Null when no provider answers inside the timeout; a revoked permission
    // surfaces as a `SecurityException` from the manager and reads as null too.
    suspend fun currentLocation(): GeoPoint? {
        if (!isGranted()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.firstOrNull { manager.allProviders.contains(it) && manager.isProviderEnabled(it) } ?: return null
        return withTimeoutOrNull(FIX_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                try {
                    LocationManagerCompat.getCurrentLocation(manager, provider, signal, context.mainExecutor) { fix ->
                        if (continuation.isActive) {
                            continuation.resume(fix?.let { GeoPoint(it.latitude, it.longitude) })
                        }
                    }
                } catch (_: SecurityException) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        }
    }

    private companion object {
        const val FIX_TIMEOUT_MS = 15_000L
    }
}
