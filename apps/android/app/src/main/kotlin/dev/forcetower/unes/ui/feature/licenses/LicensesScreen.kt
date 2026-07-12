package dev.forcetower.unes.ui.feature.licenses

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.components.LicensesFilterChipsRow
import dev.forcetower.unes.ui.feature.licenses.components.LicensesGroupCard
import dev.forcetower.unes.ui.feature.licenses.components.LicensesHero
import dev.forcetower.unes.ui.feature.licenses.components.LicensesSbomCard
import dev.forcetower.unes.ui.feature.licenses.components.LicensesSearchBar
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo

// "Licenças" — index of every dependency bundled in the APK, rebuilt as a
// native Android / Material 3 screen (dc `UNES Licenças - Android`).
//
// Data flow is unchanged: Licensee writes `artifacts.json` at build time (see
// `apps/android/app/build.gradle.kts`); AGP bundles it as an asset; the view
// model parses it once on startup and also exposes the raw manifest size for
// the export row. When the asset is missing (a fresh checkout that hasn't
// built yet) the screen surfaces an explicit empty-build-artifact card.
//
// Layout: M3 top app bar + large "Licenças" headline, a "Código aberto" hero
// (package count + segmented distribution bar), a filled search + filter chips,
// per-family group cards with expandable rows, then the manifest export row and
// a closing compliance footer. Built on `LazyColumn` so off-screen group cards
// stay out of composition.
@Composable
internal fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: LicensesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val appInfo = rememberAppInfo()

    LicensesContent(
        packages = state.packages,
        manifestSizeBytes = state.manifestSizeBytes,
        appVersion = appInfo.version,
        appBuild = appInfo.build,
        onBack = onBack,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun LicensesContent(
    packages: List<LicensePackage>?,
    manifestSizeBytes: Int?,
    appVersion: String,
    appBuild: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable(stateSaver = LicenseFilterSaver) {
        mutableStateOf<LicenseFilter>(LicenseFilter.All)
    }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val pkgs = packages.orEmpty()
    val breakdown = remember(pkgs) { computeBreakdown(pkgs) }
    val totalByFamily = remember(breakdown) { breakdown.associate { it.family to it.count } }
    val groups = remember(pkgs, query, filter) {
        groupFiltered(packages = pkgs, query = query, filter = filter, breakdown = breakdown)
    }

    val loaded = packages != null
    val anyPackages = pkgs.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LicensesTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = bottomInset + 28.dp),
        ) {
            item(key = "headline") {
                Headline(modifier = Modifier.fadeUpOnAppear(delayMs = 20))
            }

            when {
                !loaded -> item(key = "loading") { LoadingBlock() }
                !anyPackages -> item(key = "empty-artifact") {
                    Padded(horizontal = 16.dp, top = 22.dp) { EmptyBuildArtifactCard() }
                }
                else -> {
                    item(key = "hero") {
                        Padded(horizontal = 16.dp, top = 18.dp) {
                            LicensesHero(
                                totalPackages = pkgs.size,
                                breakdown = breakdown,
                                modifier = Modifier.fadeUpOnAppear(delayMs = 80),
                            )
                        }
                    }

                    item(key = "controls") {
                        Padded(horizontal = 20.dp, top = 26.dp) {
                            DependenciesControls(
                                query = query,
                                onQueryChange = { query = it },
                                modifier = Modifier.fadeUpOnAppear(delayMs = 140),
                            )
                        }
                    }

                    // Full-bleed so the chip row scrolls to the screen edge
                    // rather than clipping inside the 20.dp control inset.
                    item(key = "chips") {
                        LicensesFilterChipsRow(
                            breakdown = breakdown,
                            filter = filter,
                            onFilterChange = { filter = it },
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fadeUpOnAppear(delayMs = 170),
                        )
                    }

                    if (groups.isEmpty()) {
                        item(key = "empty-search") {
                            Padded(horizontal = 20.dp, top = 20.dp) { EmptySearchBlock() }
                        }
                    } else {
                        items(groups, key = { "group-${it.family.name}" }) { group ->
                            Padded(horizontal = 16.dp, top = 18.dp) {
                                LicensesGroupCard(
                                    family = group.family,
                                    items = group.items,
                                    total = totalByFamily[group.family] ?: group.items.size,
                                    expandedKey = expandedKey,
                                    onToggleExpanded = { key ->
                                        expandedKey = if (expandedKey == key) null else key
                                    },
                                )
                            }
                        }
                    }

                    manifestSizeBytes?.let { size ->
                        item(key = "sbom") {
                            Padded(horizontal = 16.dp, top = 20.dp) {
                                LicensesSbomCard(sizeBytes = size)
                            }
                        }
                    }

                    item(key = "footer") {
                        Padded(horizontal = 20.dp, top = 24.dp) {
                            Footer(version = appVersion, build = appBuild)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicensesTopBar(onBack: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    val ink2 = MaterialTheme.colorScheme.onSurface
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant

    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.licenses_topbar_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.licenses_back),
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = background,
            scrolledContainerColor = background,
            navigationIconContentColor = ink2,
            titleContentColor = ink3,
        ),
    )
}

@Composable
private fun Headline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp, bottom = 2.dp),
    ) {
        Text(
            text = stringResource(R.string.licenses_title),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 34.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.85).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.licenses_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DependenciesControls(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.licenses_section_dependencies).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(14.dp))
        LicensesSearchBar(query = query, onQueryChange = onQueryChange)
    }
}

@Composable
private fun Footer(version: String, build: String) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    // The footer format adds its own "v" prefix; drop a leading v/V so a
    // versionName that already carries one (e.g. "v1-legacy-android") doesn't
    // read "vv…".
    val versionLabel = version.trimStart('v', 'V')
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = stringResource(R.string.licenses_footer_version_format, versionLabel, build),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = ink4,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.licenses_footer_compliance),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = ink4,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LoadingBlock() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun EmptySearchBlock() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.SearchOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(42.dp),
        )
        Text(
            text = stringResource(R.string.licenses_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.licenses_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun EmptyBuildArtifactCard() {
    val card = MaterialTheme.melon.surface.card
    val line = MaterialTheme.melon.surface.line
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, line, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.licenses_empty_artifact_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
            color = ink,
        )
        Text(
            text = stringResource(R.string.licenses_empty_artifact_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 20.sp),
            color = ink3,
        )
    }
}

// Per-item wrapper: LazyColumn doesn't take `verticalArrangement = spacedBy`,
// so each item carries its own horizontal padding + top spacing. Horizontal
// padding is 20.dp for text/control blocks and 16.dp for cards, matching the dc
// spec's text-vs-card inset.
@Composable
private fun Padded(
    horizontal: Dp = 16.dp,
    top: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontal)
            .padding(top = top),
    ) {
        content()
    }
}

// ─────── Pure helpers ───────

private fun computeBreakdown(packages: List<LicensePackage>): List<LicenseBreakdown> {
    if (packages.isEmpty()) return emptyList()
    val counts = packages
        .groupingBy { LicenseFamily.from(it.licenseId ?: it.licenseName) }
        .eachCount()
    return counts.entries
        .map { (family, count) -> LicenseBreakdown(family, count) }
        .sortedWith(
            compareByDescending<LicenseBreakdown> { it.count }
                .thenBy { it.family.displayName.lowercase() },
        )
}

private fun groupFiltered(
    packages: List<LicensePackage>,
    query: String,
    filter: LicenseFilter,
    breakdown: List<LicenseBreakdown>,
): List<LicenseGroup> {
    val needle = query.trim().lowercase()
    val filtered = packages.filter { pkg ->
        val family = LicenseFamily.from(pkg.licenseId ?: pkg.licenseName)
        val passesFilter = filter is LicenseFilter.All ||
            (filter is LicenseFilter.Family && filter.value == family)
        if (!passesFilter) return@filter false
        if (needle.isEmpty()) return@filter true
        pkg.artifact.lowercase().contains(needle) ||
            pkg.groupId.lowercase().contains(needle) ||
            (pkg.licenseId?.lowercase()?.contains(needle) == true)
    }
    val byFamily = filtered.groupBy { LicenseFamily.from(it.licenseId ?: it.licenseName) }
    return breakdown.mapNotNull { row ->
        val items = byFamily[row.family]?.sortedBy { it.artifact.lowercase() }.orEmpty()
        if (items.isEmpty()) null else LicenseGroup(row.family, items)
    }
}

// `LicenseFilter` is a sealed type, so `rememberSaveable` needs an explicit
// saver. We collapse it to a single string ("__all__" or the family name) so
// the saved state survives process death without serialising the whole sealed
// hierarchy.
private val LicenseFilterSaver = androidx.compose.runtime.saveable.Saver<LicenseFilter, String>(
    save = {
        when (it) {
            is LicenseFilter.All -> "__all__"
            is LicenseFilter.Family -> it.value.name
        }
    },
    restore = { value ->
        if (value == "__all__") {
            LicenseFilter.All
        } else {
            val family = LicenseFamily.entries.firstOrNull { it.name == value }
            if (family != null) LicenseFilter.Family(family) else LicenseFilter.All
        }
    },
)

// ─────── Previews ───────

@Preview(name = "Licenças · claro", heightDp = 1200)
@Composable
private fun LicensesPreviewLight() {
    MelonTheme(darkTheme = false) {
        LicensesContent(
            packages = previewLicensePackages,
            manifestSizeBytes = 138_491,
            appVersion = "0.13.0",
            appBuild = "1",
            onBack = {},
        )
    }
}

@Preview(name = "Licenças · escuro", heightDp = 1200)
@Composable
private fun LicensesPreviewDark() {
    MelonTheme(darkTheme = true) {
        LicensesContent(
            packages = previewLicensePackages,
            manifestSizeBytes = 138_491,
            appVersion = "0.13.0",
            appBuild = "1",
            onBack = {},
        )
    }
}

private val previewLicensePackages: List<LicensePackage> = listOf(
    LicensePackage(
        "androidx.compose.ui:ui", "ui", "1.9.0", "androidx.compose.ui",
        "Apache-2.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0",
        "https://cs.android.com/androidx/platform/frameworks/support",
    ),
    LicensePackage(
        "com.squareup.okhttp3:okhttp", "okhttp", "4.12.0", "com.squareup.okhttp3",
        "Apache-2.0", "Apache License 2.0", "https://www.apache.org/licenses/LICENSE-2.0",
        "https://github.com/square/okhttp",
    ),
    LicensePackage(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core", "kotlinx-coroutines-core", "1.9.0",
        "org.jetbrains.kotlinx", "Apache-2.0", null, "https://www.apache.org/licenses/LICENSE-2.0",
        "https://github.com/Kotlin/kotlinx.coroutines",
    ),
    LicensePackage(
        "com.google.code.gson:gson", "gson", "2.11.0", "com.google.code.gson",
        "Apache-2.0", null, "https://www.apache.org/licenses/LICENSE-2.0",
        "https://github.com/google/gson",
    ),
    LicensePackage(
        "org.slf4j:slf4j-api", "slf4j-api", "2.0.13", "org.slf4j",
        "MIT", "MIT License", "https://opensource.org/license/mit",
        "https://github.com/qos-ch/slf4j",
    ),
    LicensePackage(
        "com.google.protobuf:protobuf-javalite", "protobuf-javalite", "3.25.3",
        "com.google.protobuf", "BSD-3-Clause", "BSD 3-Clause", "https://opensource.org/licenses/BSD-3-Clause",
        "https://github.com/protocolbuffers/protobuf",
    ),
)
