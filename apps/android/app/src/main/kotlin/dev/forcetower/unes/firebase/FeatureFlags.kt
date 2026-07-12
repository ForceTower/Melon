package dev.forcetower.unes.firebase

import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfigSettings
import dev.forcetower.unes.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

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
    val documentCaptchaSiteKey: String = "",
    val documentCaptchaBaseUrl: String = "",
)

// Firebase Remote Config bridge — the Android analogue of iOS
// `AppDelegate.configureRemoteConfig` + `FeatureFlags`. Parameter keys are
// the un-prefixed names shared with iOS (`enable_*`); the Android console
// values are managed independently, so a feature can ship on one platform
// before the other. `start()` fetch-activates on launch and subscribes to
// realtime config updates; between launches Remote Config's own disk cache
// keeps the last activated values, so gates hold their state offline.
@Singleton
internal class FeatureFlags @Inject constructor() {

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private val gatesFlow = MutableStateFlow(readGates())
    val gates: StateFlow<FeatureGates> = gatesFlow

    fun start() {
        if (BuildConfig.DEBUG) {
            // Same as iOS DEBUG: skip the 12h fetch cache so console changes
            // land on next launch while developing.
            remoteConfig.setConfigSettingsAsync(
                remoteConfigSettings { minimumFetchIntervalInSeconds = 0 },
            )
        }
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Timber.w(task.exception, "remote config fetchAndActivate failed")
            }
            gatesFlow.value = readGates()
        }
        remoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                remoteConfig.activate().addOnCompleteListener {
                    gatesFlow.value = readGates()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Timber.w(error, "remote config realtime update failed")
            }
        })
    }

    private fun readGates(): FeatureGates {
        val captchaSiteKey = remoteConfig.getString(KEY_DOCUMENT_CAPTCHA_SITE_KEY)
        val captchaBaseUrl = remoteConfig.getString(KEY_DOCUMENT_CAPTCHA_BASE_URL)
        if (BuildConfig.DEBUG) {
            return FeatureGates(
                enrollment = true,
                enrollmentCertificate = true,
                academicHistory = true,
                paradoxo = true,
                materials = true,
                documentCaptchaSiteKey = captchaSiteKey,
                documentCaptchaBaseUrl = captchaBaseUrl,
            )
        }
        return FeatureGates(
            enrollment = remoteConfig.getBoolean(KEY_ENROLLMENT),
            enrollmentCertificate = remoteConfig.getBoolean(KEY_ENROLLMENT_CERTIFICATE),
            academicHistory = remoteConfig.getBoolean(KEY_ACADEMIC_HISTORY),
            paradoxo = remoteConfig.getBoolean(KEY_PARADOXO),
            materials = remoteConfig.getBoolean(KEY_MATERIALS),
            documentCaptchaSiteKey = captchaSiteKey,
            documentCaptchaBaseUrl = captchaBaseUrl,
        )
    }

    private companion object {
        const val KEY_ENROLLMENT = "enable_enrollment"
        const val KEY_ENROLLMENT_CERTIFICATE = "enable_enrollment_certificate"
        const val KEY_ACADEMIC_HISTORY = "enable_academic_history"
        const val KEY_PARADOXO = "enable_paradoxo"
        const val KEY_MATERIALS = "enable_materials"
        const val KEY_DOCUMENT_CAPTCHA_SITE_KEY = "document_captcha_site_key"
        const val KEY_DOCUMENT_CAPTCHA_BASE_URL = "document_captcha_base_url"
    }
}
