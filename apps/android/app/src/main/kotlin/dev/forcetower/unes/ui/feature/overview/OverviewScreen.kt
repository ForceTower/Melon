package dev.forcetower.unes.ui.feature.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.ui.feature.overview.components.DisciplinesStrip
import dev.forcetower.unes.ui.feature.overview.components.NowCard
import dev.forcetower.unes.ui.feature.overview.components.OverviewHeader
import dev.forcetower.unes.ui.feature.overview.components.OverviewTileGrid
import dev.forcetower.unes.ui.feature.overview.components.TodayTimeline
import java.time.LocalTime

// "Hoje" tab — the at-a-glance dashboard rendered as the first tab inside
// `ConnectedScreen`. Currently driven by `OverviewFixtures` so the prototype,
// iOS, and Android stay visually aligned. A KMP-backed ViewModel will replace
// the fixtures once the data layer catches up — wire it through this same
// component tree so animation choreography stays put.
@Composable
fun OverviewScreen(
    modifier: Modifier = Modifier,
    bottomInset: androidx.compose.ui.unit.Dp = 0.dp,
) {
    val surface = MaterialTheme.colorScheme.surface
    val now = OverviewFixtures.nowClass()
    val today = OverviewFixtures.today()
    val disciplines = OverviewFixtures.disciplines()

    Box(modifier = modifier
        .fillMaxSize()
        .background(surface)) {
        // Ambient warm mesh pinned to the top — content scrolls over it via
        // the absolute Box stacking instead of laying the mesh in-flow above
        // the column. Mirrors iOS `OverviewView.screenBody`.
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            AmbientMeshTop(surface = surface)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                // Horizontal-only safe-drawing inset keeps content out of side
                // cutouts and the landscape nav bar without cropping the mesh,
                // which intentionally bleeds edge to edge behind the content.
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OverviewHeader(
                greeting = greeting(),
                name = stringResource(R.string.overview_default_user),
                avatarInitial = avatarInitial(),
                dateEyebrow = OverviewFixtures.DATE_EYEBROW,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                NowCard(
                    now = now,
                    modifier = Modifier.scaleInOnAppear(delayMs = 120, fromScale = 0.985f),
                )
                TodayTimeline(
                    items = today,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 240),
                )
                OverviewTileGrid(
                    grade = OverviewFixtures.gradeTile,
                    messages = OverviewFixtures.messagesTile,
                    nextTest = OverviewFixtures.nextTestTile,
                    attendance = OverviewFixtures.attendanceTile,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 340),
                )
            }

            DisciplinesStrip(
                items = disciplines,
                semesterLabel = OverviewFixtures.SEMESTER_LABEL,
                modifier = Modifier.fadeUpOnAppear(delayMs = 440),
            )

            Text(
                text = stringResource(R.string.overview_last_updated_format, OverviewFixtures.LAST_UPDATED_MINUTES),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    letterSpacing = 1.26.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .fadeUpOnAppear(delayMs = 520)
                    .padding(top = 8.dp, bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AmbientMeshTop(surface: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.2f,
            modifier = Modifier.fillMaxSize(),
        )
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
}

@Composable
private fun greeting(): String {
    val hour = LocalTime.now().hour
    val resId = when {
        hour < 12 -> R.string.overview_greeting_morning
        hour < 18 -> R.string.overview_greeting_afternoon
        else -> R.string.overview_greeting_evening
    }
    return stringResource(resId)
}

@Composable
private fun avatarInitial(): String {
    val name = stringResource(R.string.overview_default_user)
    return name.firstOrNull()?.uppercase() ?: "?"
}
