package dev.forcetower.unes.ui.feature.licenses

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Reads `artifacts.json` from the APK assets — the file is emitted by the
// Licensee Gradle plugin (see `apps/android/app/build.gradle.kts`) and copied
// into assets via `androidComponents.onVariants`. If the asset is missing
// (e.g. a fresh checkout that hasn't run the build) we degrade to an empty
// list rather than crash; the screen handles the empty state explicitly.
internal object LicensesAssetLoader {
    private const val ASSET_NAME = "artifacts.json"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    fun load(context: Context): List<LicensePackage> {
        val raw = rawManifest(context)?.decodeToString() ?: return emptyList()

        val artifacts: List<LicenseeArtifact> = runCatching {
            json.decodeFromString<List<LicenseeArtifact>>(raw)
        }.getOrDefault(emptyList())

        return artifacts.mapNotNull { it.toPackage() }.distinctBy { it.coordinates }
    }

    // Raw bytes of the bundled `artifacts.json` — the license manifest the
    // "Baixar manifesto" action exports verbatim (so what the user saves is
    // exactly what shipped in the APK, not a re-serialisation). `null` when the
    // asset is missing (fresh checkout before the Licensee task has run).
    fun rawManifest(context: Context): ByteArray? = runCatching {
        context.assets.open(ASSET_NAME).use { it.readBytes() }
    }.getOrNull()

    private fun LicenseeArtifact.toPackage(): LicensePackage? {
        val group = groupId.orEmpty()
        val artifact = artifactId.orEmpty()
        if (group.isEmpty() && artifact.isEmpty()) return null

        val spdx = spdxLicenses.firstOrNull()
        val unknown = unknownLicenses.firstOrNull()
        val licenseId = spdx?.identifier?.takeIf { it.isNotBlank() }
        val licenseName = spdx?.name ?: unknown?.name
        val licenseUrl = spdx?.url ?: unknown?.url
        val coordinates = listOf(group, artifact).filter { it.isNotEmpty() }.joinToString(":")

        return LicensePackage(
            coordinates = coordinates,
            artifact = artifact.ifBlank { group },
            version = version.orEmpty(),
            groupId = group,
            licenseId = licenseId,
            licenseName = licenseName,
            licenseUrl = licenseUrl,
            scmUrl = scm?.url?.takeIf { it.isNotBlank() },
        )
    }
}

// Exact shape Licensee writes — we only consume a subset, the rest is ignored
// via `ignoreUnknownKeys`. Kept private so the screen never sees the raw
// artifact tree.
@Serializable
private data class LicenseeArtifact(
    val groupId: String? = null,
    val artifactId: String? = null,
    val version: String? = null,
    val name: String? = null,
    val spdxLicenses: List<LicenseeSpdx> = emptyList(),
    val unknownLicenses: List<LicenseeUnknown> = emptyList(),
    val scm: LicenseeScm? = null,
)

@Serializable
private data class LicenseeSpdx(
    val identifier: String? = null,
    val name: String? = null,
    val url: String? = null,
)

@Serializable
private data class LicenseeUnknown(
    val name: String? = null,
    val url: String? = null,
)

@Serializable
private data class LicenseeScm(val url: String? = null)
