package dev.forcetower.unes.ui.feature.paradoxo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoSemesterMean
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoStats
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTaughtDiscipline
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoTeacherDetail
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoCard
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoDistribution
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoDonut
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoFailure
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoListRow
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoLoading
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoOutcomes
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoShapeChip
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoSparkline
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoTierChip
import dev.forcetower.unes.ui.feature.paradoxo.components.paradoxoTone

// Teacher detail — donut hero with the all-time mean, approval/consistency
// stat tiles, student outcomes, the grade signature histogram and the
// disciplines the teacher lectures (dc `ParadoxoScreen` teacher stage, iOS
// `ParadoxoTeacherView`).
@Composable
internal fun ParadoxoTeacherScreen(
    id: String,
    seedName: String?,
    onBack: () -> Unit,
    onOpenDiscipline: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: ParadoxoViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { vm.onIntent(ParadoxoIntent.LoadTeacher(id)) }

    val detail = state.teachers[id]
    val loaded = (detail as? ParadoxoDetail.Loaded)?.data

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (loaded != null) {
            ParadoxoWash(tone = paradoxoTone(loaded.mean))
        }
        // The app bar stays pinned; the hero and the stats scroll beneath it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            ParadoxoDetailBar(
                title = loaded?.name ?: seedName.orEmpty(),
                onBack = onBack,
                modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
            )
            PinnedHeaderHairline(scrolled = scrolled)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomInset),
            ) {
                when (detail) {
                    is ParadoxoDetail.Loaded -> ParadoxoTeacherContent(
                        detail = detail.data,
                        onOpenDiscipline = onOpenDiscipline,
                    )
                    ParadoxoDetail.Failed -> ParadoxoFailure(
                        onRetry = { vm.onIntent(ParadoxoIntent.RetryTeacher(id)) },
                    )
                    else -> ParadoxoLoading()
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun ParadoxoTeacherContent(
    detail: ParadoxoTeacherDetail,
    onOpenDiscipline: (id: String, name: String) -> Unit,
) {
    val tone = paradoxoTone(detail.mean)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        TeacherHero(
            detail = detail,
            tone = tone,
            modifier = Modifier
                .padding(top = 8.dp)
                .scaleInOnAppear(delayMs = 40, fromScale = 0.97f),
        )

        TeacherStatTiles(
            detail = detail,
            modifier = Modifier
                .padding(top = 18.dp)
                .fadeUpOnAppear(delayMs = 140),
        )

        ParadoxoCard(
            modifier = Modifier
                .padding(top = 18.dp)
                .fadeUpOnAppear(delayMs = 200),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.paradoxo_teacher_outcomes),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.32).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
                ParadoxoOutcomes(
                    approved = detail.approved,
                    failed = detail.failed,
                    quit = detail.quit,
                )
            }
        }

        if (detail.distribution.isNotEmpty()) {
            ParadoxoCard(
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fadeUpOnAppear(delayMs = 260),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(R.string.paradoxo_teacher_signature),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.32).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        ParadoxoShapeChip(
                            shape = ParadoxoStats.shapeKind(detail.distribution),
                            tone = tone,
                        )
                    }
                    ParadoxoDistribution(
                        distribution = detail.distribution,
                        tone = tone,
                        myGrade = null,
                        modifier = Modifier.padding(top = 8.dp),
                        chartHeight = 112.dp,
                    )
                    Text(
                        text = stringResource(
                            R.string.paradoxo_teacher_signature_caption_format,
                            ParadoxoFormat.count(detail.studentCount),
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                    )
                }
            }
        }

        if (detail.disciplines.isNotEmpty()) {
            Column(modifier = Modifier.fadeUpOnAppear(delayMs = 320)) {
                Text(
                    text = stringResource(R.string.paradoxo_teacher_disciplines),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.42).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 24.dp),
                )
                Text(
                    text = stringResource(R.string.paradoxo_teacher_disciplines_note),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp),
                )
                ParadoxoCard {
                    detail.disciplines.forEachIndexed { index, taught ->
                        ParadoxoListRow(
                            mean = taught.mean,
                            title = taught.name,
                            subtitle = buildAnnotatedString {
                                append(taught.code)
                                append(" · ")
                                append(
                                    stringResource(
                                        R.string.paradoxo_samples_format,
                                        ParadoxoFormat.count(taught.sampleCount),
                                    ),
                                )
                            },
                            onClick = { onOpenDiscipline(taught.id, taught.name) },
                            tileSize = 46.dp,
                            showDivider = index > 0,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeacherHero(
    detail: ParadoxoTeacherDetail,
    tone: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    ParadoxoCard(modifier = modifier) {
        Box {
            // Tonal glow bleeding from the top-right corner (dc `glowStyle`).
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-50).dp)
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(tone.copy(alpha = 0.12f)),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ParadoxoDonut(
                    mean = detail.mean,
                    caption = stringResource(
                        R.string.paradoxo_students_format,
                        ParadoxoFormat.count(detail.studentCount),
                    ),
                    tone = tone,
                )
                Text(
                    text = detail.name,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 21.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.42).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp),
                )
                ParadoxoTierChip(
                    mean = detail.mean,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun TeacherStatTiles(detail: ParadoxoTeacherDetail, modifier: Modifier = Modifier) {
    val approval = ParadoxoStats.approvalPercent(detail.approved, detail.failed, detail.quit)
    val consistency = ParadoxoStats.consistency(detail.history.map { it.mean })
    val teal = MaterialTheme.melon.palette.teal

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatTile(
            label = stringResource(R.string.paradoxo_teacher_approval),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Text(
                text = ParadoxoFormat.percent(approval),
                style = statValueStyle(),
                color = if (approval >= 60) {
                    MaterialTheme.melon.status.ok
                } else {
                    MaterialTheme.melon.status.bad
                },
            )
        }
        StatTile(
            label = stringResource(R.string.paradoxo_teacher_consistency),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Text(
                text = consistency?.let { ParadoxoFormat.percent(it) } ?: "—",
                style = statValueStyle(),
                color = teal,
            )
            if (detail.history.size >= 2) {
                ParadoxoSparkline(
                    values = detail.history.map { it.mean },
                    color = teal,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 54.dp, height = 14.dp),
                    strokeWidth = 1.75.dp,
                )
            }
        }
        StatTile(
            label = stringResource(R.string.paradoxo_teacher_last_semester),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
        ) {
            Text(
                text = detail.lastSemester ?: "—",
                style = statValueStyle(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun ParadoxoTeacherScreenPreview() {
    MelonTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
        ) {
            ParadoxoTeacherContent(
                detail = ParadoxoTeacherDetail(
                    id = "t1",
                    name = "Joilma Silva Carneiro",
                    mean = 3.5,
                    studentCount = 1335,
                    approved = 542,
                    failed = 793,
                    quit = 112,
                    lastSemester = "2025.2",
                    history = listOf(
                        ParadoxoSemesterMean("2023.1", 4.2),
                        ParadoxoSemesterMean("2023.2", 3.8),
                        ParadoxoSemesterMean("2024.1", 3.6),
                        ParadoxoSemesterMean("2024.2", 3.5),
                        ParadoxoSemesterMean("2025.1", 3.3),
                        ParadoxoSemesterMean("2025.2", 3.5),
                    ),
                    distribution = listOf(
                        0.28, 0.22, 0.18, 0.12, 0.08, 0.05, 0.03, 0.02, 0.01, 0.005, 0.005,
                    ),
                    disciplines = listOf(
                        ParadoxoTaughtDiscipline(
                            id = "d1",
                            code = "EXA704",
                            name = "Cálculo Diferencial e Integral I",
                            mean = 2.8,
                            sampleCount = 324,
                        ),
                    ),
                ),
                onOpenDiscipline = { _, _ -> },
            )
        }
    }
}

@Composable
private fun statValueStyle() = MaterialTheme.typography.headlineSmall.copy(
    fontSize = 24.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.72).sp,
)

@Composable
private fun StatTile(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ParadoxoCard(modifier = modifier, cornerRadius = 18.dp) {
        Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.42.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            content()
        }
    }
}
