package dev.forcetower.unes.ui.feature.paradoxo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineDetail
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoDisciplineTeacher
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoSemesterMean
import dev.forcetower.melon.feature.paradoxo.domain.model.ParadoxoStats
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoCard
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoDistribution
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoFailure
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoHistoryChart
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoLoading
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoOutcomes
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoScoreTile
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoShapeChip
import dev.forcetower.unes.ui.feature.paradoxo.components.ParadoxoTierChip
import dev.forcetower.unes.ui.feature.paradoxo.components.paradoxoShapeLabel
import dev.forcetower.unes.ui.feature.paradoxo.components.paradoxoTone

// Discipline detail — tinted hero with the all-time mean and outcomes,
// semester-mean chart, grade distribution against the student's own grade,
// curiosidades and the per-teacher breakdown (dc `ParadoxoScreen` discipline
// stage, iOS `ParadoxoDisciplineView`).
@Composable
internal fun ParadoxoDisciplineScreen(
    id: String,
    seedName: String?,
    onBack: () -> Unit,
    onOpenTeacher: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: ParadoxoViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(id) { vm.onIntent(ParadoxoIntent.LoadDiscipline(id)) }

    val detail = state.disciplines[id]
    val loaded = (detail as? ParadoxoDetail.Loaded)?.data

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (loaded != null) {
            ParadoxoWash(tone = paradoxoTone(loaded.mean))
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = bottomInset),
        ) {
            ParadoxoDetailBar(
                title = loaded?.code ?: seedName.orEmpty(),
                onBack = onBack,
            )
            when (detail) {
                is ParadoxoDetail.Loaded -> ParadoxoDisciplineContent(
                    detail = detail.data,
                    onOpenTeacher = onOpenTeacher,
                )
                ParadoxoDetail.Failed -> ParadoxoFailure(
                    onRetry = { vm.onIntent(ParadoxoIntent.RetryDiscipline(id)) },
                )
                else -> ParadoxoLoading()
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

// Soft severity-tinted radial wash bleeding from the top edge (dc `wash`).
@Composable
internal fun ParadoxoWash(tone: Color, modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(tone.copy(alpha = 0.09f), Color.Transparent),
                center = Offset(size.width / 2f, 0f),
                radius = size.width * 0.72f,
            ),
        )
    }
}

// Shared detail top bar: back button + centered title.
@Composable
internal fun ParadoxoDetailBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(R.string.paradoxo_back)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 6.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = backLabel, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = backLabel,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(24.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.17).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(44.dp))
    }
}

@Composable
private fun ParadoxoDisciplineContent(
    detail: ParadoxoDisciplineDetail,
    onOpenTeacher: (id: String, name: String) -> Unit,
) {
    val tone = paradoxoTone(detail.mean)

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        DisciplineHero(
            detail = detail,
            tone = tone,
            modifier = Modifier
                .padding(top = 8.dp)
                .scaleInOnAppear(delayMs = 40, fromScale = 0.97f),
        )

        if (detail.history.isNotEmpty()) {
            ParadoxoCard(modifier = Modifier
                .padding(top = 18.dp)
                .fadeUpOnAppear(delayMs = 140),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.paradoxo_chart_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.32).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    ParadoxoHistoryChart(history = detail.history, tone = tone)
                    PeakTroughTiles(
                        detail = detail,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        if (detail.distribution.isNotEmpty()) {
            DistributionCard(
                detail = detail,
                tone = tone,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .fadeUpOnAppear(delayMs = 220),
            )
        }

        if (detail.history.isNotEmpty()) {
            InsightsSection(
                detail = detail,
                modifier = Modifier.fadeUpOnAppear(delayMs = 280),
            )
        }

        if (detail.teachers.isNotEmpty()) {
            TeachersSection(
                teachers = detail.teachers,
                onOpenTeacher = onOpenTeacher,
                modifier = Modifier.fadeUpOnAppear(delayMs = 340),
            )
        }
    }
}

@Composable
private fun DisciplineHero(
    detail: ParadoxoDisciplineDetail,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val card = MaterialTheme.melon.surface.card
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(card)
            .background(
                Brush.linearGradient(
                    colors = listOf(tone.copy(alpha = 0.12f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset.Infinite,
                ),
            )
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = detail.code,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.44.sp,
                ),
                color = tone,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(tone.copy(alpha = 0.2f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            )
            val department = detail.department
            if (!department.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.paradoxo_department_format, department),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Text(
            text = detail.name,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.48).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 300.dp),
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = ParadoxoFormat.grade(detail.mean),
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 58.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-2.9).sp,
                    ),
                    color = tone,
                )
                Text(
                    text = stringResource(R.string.paradoxo_grade_out_of_ten),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(start = 3.dp, bottom = 6.dp),
                )
            }
            ParadoxoTierChip(
                mean = detail.mean,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        Text(
            text = stringResource(
                R.string.paradoxo_detail_calculated_format,
                ParadoxoFormat.count(detail.studentCount),
            ),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 8.dp),
        )
        ParadoxoOutcomes(
            approved = detail.approved,
            failed = detail.failed,
            quit = detail.quit,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun PeakTroughTiles(detail: ParadoxoDisciplineDetail, modifier: Modifier = Modifier) {
    val history = detail.history
    val peak = history.maxBy { it.mean }
    val trough = history.minBy { it.mean }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PeakTroughTile(
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            tone = MaterialTheme.melon.status.ok,
            label = stringResource(R.string.paradoxo_chart_peak),
            value = stringResource(
                R.string.paradoxo_chart_point_format,
                ParadoxoFormat.grade(peak.mean),
                peak.semester,
            ),
            modifier = Modifier.weight(1f),
        )
        PeakTroughTile(
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            tone = MaterialTheme.melon.status.bad,
            label = stringResource(R.string.paradoxo_chart_trough),
            value = stringResource(
                R.string.paradoxo_chart_point_format,
                ParadoxoFormat.grade(trough.mean),
                trough.semester,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PeakTroughTile(
    icon: ImageVector,
    tone: Color,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tone.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(15.dp),
            )
        }
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.42.sp,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun DistributionCard(
    detail: ParadoxoDisciplineDetail,
    tone: Color,
    modifier: Modifier = Modifier,
) {
    ParadoxoCard(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.paradoxo_dist_title),
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
                myGrade = detail.myGrade,
                modifier = Modifier.padding(top = 8.dp),
            )
            val myGrade = detail.myGrade
            if (myGrade != null) {
                val percentile = ParadoxoStats.percentile(detail.distribution, myGrade)
                Text(
                    text = stringResource(
                        R.string.paradoxo_dist_top_percent_format,
                        ParadoxoFormat.percent(100 - percentile),
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun InsightsSection(detail: ParadoxoDisciplineDetail, modifier: Modifier = Modifier) {
    val history = detail.history
    val peak = history.maxBy { it.mean }
    val trough = history.minBy { it.mean }
    val teacherSpread = if (detail.teachers.size > 1) {
        detail.teachers.maxOf { it.mean } - detail.teachers.minOf { it.mean }
    } else {
        0.0
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.paradoxo_insights_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.42).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 12.dp),
        )
        ParadoxoCard {
            InsightRow(
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                tone = MaterialTheme.melon.status.ok,
                text = stringResource(
                    R.string.paradoxo_insight_peak_format,
                    peak.semester,
                    ParadoxoFormat.grade(peak.mean),
                ),
            )
            InsightRow(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                tone = MaterialTheme.melon.status.bad,
                text = stringResource(
                    R.string.paradoxo_insight_trough_format,
                    trough.semester,
                    ParadoxoFormat.grade(trough.mean),
                ),
                showDivider = true,
            )
            if (teacherSpread > 1.5) {
                InsightRow(
                    icon = Icons.Filled.Contrast,
                    tone = MaterialTheme.melon.palette.magenta,
                    text = stringResource(
                        R.string.paradoxo_insight_gap_format,
                        ParadoxoFormat.grade(teacherSpread),
                    ),
                    showDivider = true,
                )
            }
        }
    }
}

@Composable
private fun InsightRow(
    icon: ImageVector,
    tone: Color,
    text: String,
    showDivider: Boolean = false,
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
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tone.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TeachersSection(
    teachers: List<ParadoxoDisciplineTeacher>,
    onOpenTeacher: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.paradoxo_teachers_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.42).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(start = 4.dp, top = 24.dp),
        )
        Text(
            text = stringResource(R.string.paradoxo_teachers_note),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 12.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            teachers.forEach { teacher ->
                TeacherCard(teacher = teacher, onOpenTeacher = onOpenTeacher)
            }
        }
    }
}

@Preview
@Composable
private fun ParadoxoDisciplineScreenPreview() {
    MelonTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
        ) {
            ParadoxoDisciplineContent(
                detail = ParadoxoDisciplineDetail(
                    id = "d1",
                    code = "EXA704",
                    name = "Cálculo Diferencial e Integral I",
                    department = "Ciências Exatas",
                    mean = 3.5,
                    studentCount = 6868,
                    approved = 2477,
                    failed = 3743,
                    quit = 648,
                    history = listOf(
                        ParadoxoSemesterMean("2023.1", 4.0),
                        ParadoxoSemesterMean("2023.2", 3.5),
                        ParadoxoSemesterMean("2024.1", 3.1),
                        ParadoxoSemesterMean("2024.2", 2.9),
                        ParadoxoSemesterMean("2025.1", 3.3),
                        ParadoxoSemesterMean("2025.2", 3.5),
                    ),
                    distribution = listOf(
                        0.20, 0.18, 0.16, 0.13, 0.10, 0.08, 0.06, 0.04, 0.03, 0.01, 0.01,
                    ),
                    myGrade = 7.2,
                    teachers = listOf(
                        ParadoxoDisciplineTeacher(
                            id = "t1",
                            name = "Joilma Silva Carneiro",
                            mean = 2.8,
                            sampleCount = 324,
                            lastSemester = "2025.2",
                            history = listOf(
                                ParadoxoSemesterMean("2024.1", 3.1),
                                ParadoxoSemesterMean("2024.2", 2.5),
                                ParadoxoSemesterMean("2025.1", 3.1),
                                ParadoxoSemesterMean("2025.2", 3.0),
                            ),
                        ),
                        ParadoxoDisciplineTeacher(
                            id = "t2",
                            name = "Adriana Matos",
                            mean = 5.9,
                            sampleCount = 428,
                            lastSemester = "2024.1",
                            history = emptyList(),
                        ),
                    ),
                ),
                onOpenTeacher = { _, _ -> },
            )
        }
    }
}

@Composable
private fun TeacherCard(
    teacher: ParadoxoDisciplineTeacher,
    onOpenTeacher: (id: String, name: String) -> Unit,
) {
    val expandable = teacher.history.size > 1
    var expanded by rememberSaveable(teacher.id) { mutableStateOf(false) }
    ParadoxoCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = expandable) { expanded = !expanded }
                .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            ParadoxoScoreTile(mean = teacher.mean, size = 46.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = teacher.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.31).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val samples = ParadoxoFormat.count(teacher.sampleCount)
                Text(
                    text = teacher.lastSemester?.let {
                        stringResource(R.string.paradoxo_teacher_meta_format, samples, it)
                    } ?: stringResource(R.string.paradoxo_samples_format, samples),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(
                        1.dp,
                        MaterialTheme.melon.surface.line,
                        RoundedCornerShape(9.dp),
                    )
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.paradoxo_open_teacher),
                    ) { onOpenTeacher(teacher.id, teacher.name) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp),
                )
            }
            if (expandable) {
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.paradoxo_expand_history),
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(if (expanded) 180f else 0f),
                )
            }
        }
        if (expandable) {
            val line = MaterialTheme.melon.surface.line
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(tween(280, easing = MelonMotion.EmphasizedEasing)) +
                    fadeIn(tween(280, easing = MelonMotion.EmphasizedEasing)),
                exit = shrinkVertically(tween(220, easing = MelonMotion.EmphasizedEasing)) +
                    fadeOut(tween(220, easing = MelonMotion.EmphasizedEasing)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBehind {
                            drawLine(
                                color = line,
                                start = Offset.Zero,
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx(),
                            )
                        }
                        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 14.dp),
                ) {
                    ParadoxoHistoryChart(
                        history = teacher.history,
                        tone = paradoxoTone(teacher.mean),
                        plotHeight = 150.dp,
                    )
                }
            }
        }
    }
}
