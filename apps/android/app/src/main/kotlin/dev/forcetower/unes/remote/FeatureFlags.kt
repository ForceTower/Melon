package dev.forcetower.unes.remote

import dev.forcetower.unes.BuildConfig
import dev.forcetower.unes.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

// Remote-gated feature switches for the Eu shortcuts. `true` means the tile
// renders; DEBUG builds surface everything so gated features stay reachable
// during development — the exact rule iOS applies in `MeFeature.State.shortcuts`.
//
// The captcha pair gates the document-request flow: a non-empty site key
// means the portal demands a solved reCAPTCHA before issuing PDFs, rendered
// against `documentCaptchaBaseUrl` (the portal origin Google's domain
// allow-list is checked against). Both come through as-is even in DEBUG.
internal data class FeatureGates(
    val enrollment: Boolean = false,
    val enrollmentCertificate: Boolean = false,
    val academicHistory: Boolean = false,
    val paradoxo: Boolean = false,
    val materials: Boolean = false,
    val library: Boolean = false,
    val campusEvent: Boolean = false,
    val evaluationReminders: Boolean = false,
    val documentCaptchaSiteKey: String = "",
    val documentCaptchaBaseUrl: String = "",
)

// The gate projection over whatever `RemoteSettings` currently resolves to —
// in production, lever in front of Firebase Remote Config. This layer knows
// nothing about either: it reads keys and recomputes whenever a source says its
// values changed. The Android analogue of iOS `AppDelegate.publishFlags`.
@Singleton
internal class FeatureFlags @Inject constructor(
    private val settings: RemoteSettings,
    @ApplicationScope private val scope: CoroutineScope,
) {
    // Seeded before any fetch: both sources serve their last activated values
    // from disk, so gates hold their state offline and across launches.
    private val gatesFlow = MutableStateFlow(readGates())
    val gates: StateFlow<FeatureGates> = gatesFlow

    fun start() {
        settings.start()
        scope.launch {
            // The `onStart` recompute covers anything that activated between
            // construction and this subscription — lever fetches from the
            // moment its client exists, so that window is real.
            settings.changes
                .onStart { emit(Unit) }
                .collect { gatesFlow.value = readGates() }
        }
    }

    private fun readGates(): FeatureGates {
        val captchaSiteKey = settings.string(RemoteStringKey.DOCUMENT_CAPTCHA_SITE_KEY)
        val captchaBaseUrl = settings.string(RemoteStringKey.DOCUMENT_CAPTCHA_BASE_URL)
        if (BuildConfig.DEBUG) {
            return FeatureGates(
                enrollment = true,
                enrollmentCertificate = true,
                academicHistory = true,
                paradoxo = true,
                materials = true,
                library = true,
                campusEvent = true,
                evaluationReminders = true,
                documentCaptchaSiteKey = captchaSiteKey,
                documentCaptchaBaseUrl = captchaBaseUrl,
            )
        }
        return FeatureGates(
            enrollment = settings.bool(RemoteBoolKey.ENROLLMENT),
            enrollmentCertificate = settings.bool(RemoteBoolKey.ENROLLMENT_CERTIFICATE),
            academicHistory = settings.bool(RemoteBoolKey.ACADEMIC_HISTORY),
            paradoxo = settings.bool(RemoteBoolKey.PARADOXO),
            materials = settings.bool(RemoteBoolKey.MATERIALS),
            library = settings.bool(RemoteBoolKey.LIBRARY),
            campusEvent = settings.bool(RemoteBoolKey.CAMPUS_EVENT),
            evaluationReminders = settings.bool(RemoteBoolKey.EVALUATION_REMINDERS),
            documentCaptchaSiteKey = captchaSiteKey,
            documentCaptchaBaseUrl = captchaBaseUrl,
        )
    }
}
