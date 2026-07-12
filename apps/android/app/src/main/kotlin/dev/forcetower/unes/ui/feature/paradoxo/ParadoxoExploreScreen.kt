package dev.forcetower.unes.ui.feature.paradoxo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoExploreKind
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRankingEntry
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoRef
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoCard
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoFailure
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoLoading
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoScoreTile

// Explore ranking — one "Explorar" category rendered as a ranked list
// (mirrors iOS `ParadoxoExploreView`). Rankings ride along on the overview
// payload, so this screen reads from the shared ViewModel and only triggers
// a load when the user landed here without the home screen having fetched.
@Composable
internal fun ParadoxoExploreScreen(
    kind: ParadoxoExploreKind,
    onBack: () -> Unit,
    onOpenDiscipline: (id: String, name: String) -> Unit,
    onOpenTeacher: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: ParadoxoViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(ParadoxoIntent.Load) }

    val tone = paradoxoExploreTone(kind)
    val (titleRes, subRes, icon) = when (kind) {
        ParadoxoExploreKind.Brutal -> Triple(
            R.string.paradoxo_explore_brutal_title,
            R.string.paradoxo_explore_brutal_sub,
            Icons.Filled.Bolt,
        )
        ParadoxoExploreKind.Kind -> Triple(
            R.string.paradoxo_explore_kind_title,
            R.string.paradoxo_explore_kind_sub,
            Icons.Filled.WbSunny,
        )
        ParadoxoExploreKind.Rising -> Triple(
            R.string.paradoxo_explore_rising_title,
            R.string.paradoxo_explore_rising_sub,
            Icons.AutoMirrored.Filled.TrendingUp,
        )
        ParadoxoExploreKind.Gap -> Triple(
            R.string.paradoxo_explore_gap_title,
            R.string.paradoxo_explore_gap_sub,
            Icons.Filled.Contrast,
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = bottomInset),
    ) {
        ParadoxoDetailBar(title = stringResource(titleRes), onBack = onBack)

        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .fadeUpOnAppear(delayMs = 40),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(tone.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.44).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(subRes),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }

        val overview = state.overview
        val ranking = overview?.ranking(kind)
        when {
            ranking != null && ranking.entries.isNotEmpty() -> {
                ParadoxoCard(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fadeUpOnAppear(delayMs = 120),
                ) {
                    ranking.entries.forEachIndexed { index, entry ->
                        RankedRow(
                            position = index + 1,
                            entry = entry,
                            showDivider = index > 0,
                            onClick = {
                                when (val ref = entry.ref) {
                                    is ParadoxoRef.Discipline ->
                                        onOpenDiscipline(ref.id, entry.name)
                                    is ParadoxoRef.Teacher ->
                                        onOpenTeacher(ref.id, entry.name)
                                }
                            },
                        )
                    }
                }
            }
            state.failed || (overview != null && ranking == null) ->
                ParadoxoFailure(onRetry = { vm.onIntent(ParadoxoIntent.Retry) })
            else -> ParadoxoLoading()
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun RankedRow(
    position: Int,
    entry: ParadoxoRankingEntry,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) {
                    Modifier.drawBehind {
                        drawLine(
                            color = line,
                            start = Offset.Zero,
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = position.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.width(18.dp),
        )
        ParadoxoScoreTile(mean = entry.mean, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val code = entry.code
            val students = stringResource(
                R.string.paradoxo_students_format,
                ParadoxoFormat.count(entry.studentCount),
            )
            Text(
                text = if (code.isNullOrBlank()) students else "$code · $students",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        val delta = entry.delta
        if (delta != null) {
            val deltaTone = if (delta >= 0) {
                MaterialTheme.melon.status.ok
            } else {
                MaterialTheme.melon.status.bad
            }
            Text(
                text = ParadoxoFormat.signedGrade(delta),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = deltaTone,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(deltaTone.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}
