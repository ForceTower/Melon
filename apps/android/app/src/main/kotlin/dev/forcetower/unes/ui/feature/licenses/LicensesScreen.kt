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
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.licenses.components.LicensesFilterChipsRow
import dev.forcetower.unes.ui.feature.licenses.components.LicensesFooter
import dev.forcetower.unes.ui.feature.licenses.components.LicensesGroupCard
import dev.forcetower.unes.ui.feature.licenses.components.LicensesHeader
import dev.forcetower.unes.ui.feature.licenses.components.LicensesSearchBar
import dev.forcetower.unes.ui.feature.licenses.components.LicensesSummaryCard
import dev.forcetower.unes.ui.feature.licenses.components.LicensesTributeCard
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo

// "Licenças open source" — index of every dependency bundled in the APK.
//
// Data flow: Licensee writes `artifacts.json` at build time (see
// `apps/android/app/build.gradle.kts`); AGP bundles it as an asset; the
// view model loads + parses it once on startup. The screen never reaches for
// fixtures — when the asset is missing (fresh checkout that hasn't built
// yet) we surface an explicit empty-build-artifact card.
//
// Layout follows `screens-licenses.jsx` end to end and the iOS counterpart
// (`apps/ios/UNES/Features/Licenses/LicensesView.swift`): warm mesh wash
// behind the editorial header, distribution summary card, dark tribute card,
// search field + filter chips, then per-family group cards with expandable
// rows, then a closing signature.
//
// Implementation note: built on `LazyColumn` so off-screen group cards stay
// out of composition. Each chrome block (header / summary / tribute / search
// / filters / footer) is a single `item`; each license-family group is also
// a single `item` (a contiguous group preserves the rounded card chrome and
// dividers without per-row corner accounting). With ~150 deps + ~7 families
// the largest group is the only one that pays full composition cost on
// scroll-into-view, which is well under the 16 ms frame budget on debug.
@Composable
internal fun LicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: LicensesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val appInfo = rememberAppInfo()

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable(stateSaver = LicenseFilterSaver) {
        mutableStateOf<LicenseFilter>(LicenseFilter.All)
    }
    var expandedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val packages = state.packages.orEmpty()
    val breakdown = remember(packages) { computeBreakdown(packages) }
    val groups = remember(packages, query, filter) {
        groupFiltered(packages = packages, query = query, filter = filter, breakdown = breakdown)
    }

    val surface = MaterialTheme.colorScheme.surface
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val loaded = state.packages != null
    val anyPackages = packages.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Warm mesh wash pinned behind the header — same treatment as Settings
        // and FinalCountdown so the editorial type reads cleanly below.
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            Mesh(variant = MeshVariant.Warm, intensity = 0.5f, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            1f to surface,
                        ),
                    ),
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(top = statusBarTop, bottom = bottomInset + 32.dp),
        ) {
            // ── Editorial chrome ──
            // The chrome above the list is always above the fold, so we
            // keep `fadeUpOnAppear` here for the entrance animation. Items
            // below the search/filter strip drop the modifier — LazyColumn
            // recomposes items on scroll-into-view, which would re-fire the
            // fade every time a row entered the viewport.
            item(key = "header") {
                LicensesHeader(
                    onBack = onBack,
                    totalPackages = packages.size,
                    appVersion = appInfo.version,
                    appBuild = appInfo.build,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
            }

            item(key = "summary") {
                Padded {
                    LicensesSummaryCard(
                        breakdown = breakdown,
                        modifier = Modifier.fadeUpOnAppear(delayMs = 80),
                    )
                }
            }

            item(key = "tribute") {
                Padded {
                    LicensesTributeCard(modifier = Modifier.fadeUpOnAppear(delayMs = 140))
                }
            }

            item(key = "search") {
                Padded {
                    LicensesSearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        modifier = Modifier.fadeUpOnAppear(delayMs = 180),
                    )
                }
            }

            item(key = "filters") {
                Padded {
                    LicensesFilterChipsRow(
                        breakdown = breakdown,
                        filter = filter,
                        onFilterChange = { filter = it },
                        modifier = Modifier.fadeUpOnAppear(delayMs = 220),
                    )
                }
            }

            // ── List body ──
            when {
                !loaded -> item(key = "loading") {
                    Spacer(modifier = Modifier.height(180.dp))
                }
                !anyPackages -> item(key = "empty-artifact") {
                    Padded { EmptyBuildArtifactCard() }
                }
                groups.isEmpty() -> item(key = "empty-search") {
                    Padded { EmptySearchCard() }
                }
                else -> items(groups, key = { "group-${it.family.name}" }) { group ->
                    Padded(topPadding = 18.dp) {
                        LicensesGroupCard(
                            family = group.family,
                            items = group.items,
                            expandedKey = expandedKey,
                            onToggleExpanded = { key ->
                                expandedKey = if (expandedKey == key) null else key
                            },
                        )
                    }
                }
            }

            item(key = "footer") {
                Padded(topPadding = 14.dp) {
                    LicensesFooter(
                        appVersion = appInfo.version,
                        appBuild = appInfo.build,
                    )
                }
            }
        }
    }
}

// `LazyColumn` doesn't accept a `verticalArrangement = spacedBy` and per-item
// horizontal padding the same way a regular Column does, so wrap each item
// in a small helper. Default `topPadding` of 14.dp matches the previous
// `Arrangement.spacedBy(14.dp)` between chrome blocks; group items override
// it to 18.dp to mirror iOS.
@Composable
private fun Padded(
    topPadding: Dp = 14.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = topPadding),
    ) {
        content()
    }
}

@Composable
private fun EmptyBuildArtifactCard() {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.licenses_empty_artifact_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 18.sp,
                letterSpacing = (-0.18).sp,
            ),
            color = ink,
        )
        Text(
            text = stringResource(R.string.licenses_empty_artifact_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 13.sp,
                lineHeight = 20.sp,
            ),
            color = ink3,
        )
    }
}

@Composable
private fun EmptySearchCard() {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(22.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .border(1.dp, cardLine, shape)
            .padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = stringResource(R.string.licenses_empty_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.2).sp,
            ),
            color = ink,
        )
        Text(
            text = stringResource(R.string.licenses_empty_subtitle),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.0.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = ink4,
            textAlign = TextAlign.Center,
        )
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
