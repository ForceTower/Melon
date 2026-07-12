package dev.forcetower.unes.ui.feature.enrollment

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.enrollment.domain.model.EnrollmentWindowState
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentAppBar
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentDock
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentHero
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentStatTile
import dev.forcetower.unes.ui.feature.enrollment.components.EnrollmentWorkloadCard
import dev.forcetower.unes.ui.feature.enrollment.components.workloadSignal
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Matrícula status hub (dc `MatriculaScreen` status view): identity strip,
// mesh window hero, the disciplinas/conflitos/em fila stat trio, the workload
// meter and the current proposal list, with the step CTA in the dock.
@Composable
internal fun EnrollmentScreen(
    onBack: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenDiscipline: (Long) -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: EnrollmentViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.onIntent(EnrollmentIntent.Enter) }
    EnrollmentStatusContent(
        state = state,
        onBack = onBack,
        onRetry = { vm.onIntent(EnrollmentIntent.Retry) },
        onOpenOffers = onOpenOffers,
        onReopen = {
            vm.onIntent(EnrollmentIntent.Reopen)
            onOpenOffers()
        },
        onOpenDiscipline = onOpenDiscipline,
        onOpenReview = onOpenReview,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun EnrollmentStatusContent(
    state: EnrollmentUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenOffers: () -> Unit,
    onReopen: () -> Unit,
    onOpenDiscipline: (Long) -> Unit,
    onOpenReview: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val window = state.window
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset + 120.dp),
        ) {
            EnrollmentAppBar(
                title = stringResource(R.string.enrollment_title),
                onBack = onBack,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .fadeUpOnAppear(delayMs = 20),
            )

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 40)) {
                    Text(
                        text = stringResource(R.string.enrollment_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontSize = 32.sp,
                            lineHeight = 33.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.96).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    val identityLine = listOfNotNull(
                        state.studentName?.takeIf { it.isNotBlank() },
                        state.courseName?.takeIf { it.isNotBlank() },
                        state.semesterOrdinal?.let { stringResource(R.string.enrollment_identity_period_format, it) },
                    ).joinToString(" · ")
                    if (identityLine.isNotEmpty()) {
                        Text(
                            text = identityLine,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                when {
                    state.phase == EnrollmentPhase.Loading || state.phase == EnrollmentPhase.Idle -> LoadingState()
                    state.phase == EnrollmentPhase.Failed -> ErrorState(error = state.error, onRetry = onRetry)
                    window == null -> EmptyState()
                    else -> {
                        EnrollmentHero(
                            window = window,
                            nowMillis = state.referenceNowMillis,
                            modifier = Modifier.fadeUpOnAppear(delayMs = 80),
                        )

                        if (state.offersFailed) {
                            OffersFailedCard(onRetry = onRetry, modifier = Modifier.padding(top = 18.dp))
                        } else {
                            StatTiles(state = state, modifier = Modifier.padding(top = 18.dp))
                            EnrollmentWorkloadCard(
                                totalHours = state.totalHours,
                                window = window,
                                modifier = Modifier
                                    .padding(top = 18.dp)
                                    .fadeUpOnAppear(delayMs = 220),
                            )
                            if (state.resolvedPicks.isNotEmpty()) {
                                ProposalList(
                                    state = state,
                                    onEdit = onOpenOffers,
                                    onOpenDiscipline = onOpenDiscipline,
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .fadeUpOnAppear(delayMs = 280),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.phase == EnrollmentPhase.Loaded && window != null && !state.offersFailed &&
            window.state != EnrollmentWindowState.Upcoming
        ) {
            val locked = state.isReadonly
            val signal = workloadSignal(state.totalHours, window)
            val conflictCount = state.conflicts.size
            EnrollmentDock(
                totalHours = state.totalHours,
                maxHours = window.maxHours,
                hoursColor = signal.color,
                subText = if (conflictCount > 0) {
                    pluralStringResource(R.plurals.enrollment_dock_conflicts, conflictCount, conflictCount)
                } else {
                    pluralStringResource(R.plurals.enrollment_dock_disciplines, state.resolvedPicks.size, state.resolvedPicks.size)
                },
                subColor = if (conflictCount > 0) MaterialTheme.melon.status.bad else MaterialTheme.colorScheme.outline,
                primaryLabel = stringResource(
                    when {
                        locked -> R.string.enrollment_cta_receipt
                        state.resolvedPicks.isEmpty() -> R.string.enrollment_cta_start
                        else -> R.string.enrollment_cta_continue
                    },
                ),
                primaryIcon = if (locked) {
                    Icons.AutoMirrored.Filled.ReceiptLong
                } else {
                    Icons.AutoMirrored.Filled.ArrowForward
                },
                primaryFillsWidth = true,
                onPrimary = if (locked) onOpenReview else onOpenOffers,
                secondaryLabel = if (locked) stringResource(R.string.enrollment_cta_reopen) else null,
                secondaryIcon = if (locked) Icons.Filled.LockOpen else null,
                onSecondary = if (locked) onReopen else null,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomInset),
            )
        }
    }
}

@Composable
private fun StatTiles(state: EnrollmentUiState, modifier: Modifier = Modifier) {
    val conflicts = state.conflicts.size
    Row(modifier = modifier.fillMaxWidth().fadeUpOnAppear(delayMs = 160), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        EnrollmentStatTile(
            label = stringResource(R.string.enrollment_stat_disciplines),
            value = state.resolvedPicks.size.toString(),
            hint = stringResource(R.string.enrollment_stat_disciplines_hint),
            modifier = Modifier.weight(1f),
        )
        EnrollmentStatTile(
            label = stringResource(R.string.enrollment_stat_conflicts),
            value = conflicts.toString(),
            hint = stringResource(
                if (conflicts > 0) R.string.enrollment_stat_conflicts_fix
                else R.string.enrollment_stat_conflicts_ok,
            ),
            valueColor = if (conflicts > 0) MaterialTheme.melon.status.bad else MaterialTheme.melon.status.ok,
            modifier = Modifier.weight(1f),
        )
        EnrollmentStatTile(
            label = stringResource(R.string.enrollment_stat_waitlist),
            value = state.waitlistedCount.toString(),
            hint = stringResource(R.string.enrollment_stat_waitlist_hint),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProposalList(
    state: EnrollmentUiState,
    onEdit: () -> Unit,
    onOpenDiscipline: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.enrollment_proposal_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.57).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (state.canEdit) {
                Text(
                    text = stringResource(R.string.enrollment_proposal_edit),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onEdit)
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val shape = RoundedCornerShape(20.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.line, shape),
        ) {
            val picks = state.resolvedPicks
            picks.forEachIndexed { index, pick ->
                val hue = ColorFor.discipline(pick.discipline.code)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenDiscipline(pick.discipline.id) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(34.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(hue),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                        ) {
                            Text(
                                text = pick.discipline.code,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.3.sp,
                                ),
                                color = hue,
                            )
                            Text(
                                text = pick.section.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                        Text(
                            text = pick.discipline.name,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.enrollment_hours_format, pick.discipline.workload),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
                if (index < picks.lastIndex) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 30.dp)
                            .height(1.dp)
                            .background(MaterialTheme.melon.surface.line),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 120.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(strokeWidth = 3.dp, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 90.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.enrollment_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.enrollment_empty_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun ErrorState(
    error: dev.forcetower.melon.feature.enrollment.domain.repository.EnrollmentError?,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 90.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.enrollment_error_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Text(
            text = enrollmentErrorMessage(error),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text(text = stringResource(R.string.enrollment_retry))
        }
    }
}

@Composable
private fun OffersFailedCard(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.enrollment_offers_failed),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
        )
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(15.dp),
            modifier = Modifier.padding(top = 14.dp),
        ) {
            Text(text = stringResource(R.string.enrollment_retry))
        }
    }
}

@Preview
@Composable
private fun EnrollmentStatusPreview() {
    MelonTheme {
        EnrollmentStatusContent(
            state = EnrollmentFixtures.state,
            onBack = {},
            onRetry = {},
            onOpenOffers = {},
            onReopen = {},
            onOpenDiscipline = {},
            onOpenReview = {},
        )
    }
}
