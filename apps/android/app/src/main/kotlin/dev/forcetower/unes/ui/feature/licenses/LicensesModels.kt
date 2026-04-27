package dev.forcetower.unes.ui.feature.licenses

// Domain shapes for the Licenças screen. The bytes that drive everything come
// from `artifacts.json` — generated at build time by the Licensee Gradle
// plugin and bundled as an asset. The loader parses that JSON into
// `LicensePackage`; the screen never touches the raw artifact shape.
//
// Mirrors `apps/ios/UNES/Features/Licenses/LicensesView.swift` data layout:
// per-family normalisation in `LicenseFamily`, breakdown rows for the summary
// card, group rows for the list. Where iOS reads from a plist (title +
// identifier + body), Licensee gives us coordinates + license + scm URL — no
// body text, but a real homepage to link to.
internal data class LicensePackage(
    val coordinates: String,
    val artifact: String,
    val version: String,
    val groupId: String,
    val licenseId: String?,
    val licenseName: String?,
    val licenseUrl: String?,
    val scmUrl: String?,
)

// Canonical license family. Licensee's `spdxLicenses[].identifier` is usually
// strict SPDX, but the unknown-licenses bucket and the occasional declared
// "Apache 2" / "BSD 3-Clause" variant still leak through, so we normalise the
// same way iOS does.
internal enum class LicenseFamily(val displayName: String, val blurb: String) {
    Mit(displayName = "MIT", blurb = "permissiva · atribuição · sem garantia"),
    Apache2(displayName = "Apache-2.0", blurb = "permissiva · patentes · atribuição"),
    Bsd3(displayName = "BSD-3-Clause", blurb = "permissiva · atribuição · sem endosso"),
    Bsd2(displayName = "BSD-2-Clause", blurb = "permissiva · atribuição"),
    Isc(displayName = "ISC", blurb = "permissiva · simplificada"),
    Mpl2(displayName = "MPL-2.0", blurb = "copyleft fraco · arquivo a arquivo"),
    Epl1(displayName = "EPL-1.0", blurb = "copyleft fraco · eclipse"),
    CcBy4(displayName = "CC-BY-4.0", blurb = "creative commons · atribuição"),
    Cc0(displayName = "CC0-1.0", blurb = "domínio público · cc zero"),
    Unlicense(displayName = "Unlicense", blurb = "domínio público · sem reserva"),
    Other(displayName = "Outras", blurb = "licença declarada");

    companion object {
        fun from(identifier: String?): LicenseFamily {
            val raw = identifier?.uppercase()?.trim().orEmpty()
            if (raw.isEmpty()) return Other
            val normalized = raw.replace(' ', '-')
            return when {
                normalized.contains("APACHE") -> Apache2
                normalized.contains("MIT") -> Mit
                normalized.contains("BSD") && normalized.contains("3") -> Bsd3
                normalized.contains("BSD") -> Bsd2
                normalized.contains("ISC") -> Isc
                normalized.contains("MPL") || normalized.contains("MOZILLA") -> Mpl2
                normalized.contains("EPL") || normalized.contains("ECLIPSE") -> Epl1
                normalized.contains("CC-BY") || normalized.contains("CREATIVE-COMMONS") -> CcBy4
                normalized.contains("CC0") -> Cc0
                normalized.contains("UNLICENSE") || normalized == "PUBLIC-DOMAIN" -> Unlicense
                else -> Other
            }
        }
    }
}

internal data class LicenseBreakdown(val family: LicenseFamily, val count: Int)

internal data class LicenseGroup(val family: LicenseFamily, val items: List<LicensePackage>)

// "todos" or one specific family. Mirrors iOS `LicenseFilter`.
internal sealed interface LicenseFilter {
    data object All : LicenseFilter
    data class Family(val value: LicenseFamily) : LicenseFilter
}
