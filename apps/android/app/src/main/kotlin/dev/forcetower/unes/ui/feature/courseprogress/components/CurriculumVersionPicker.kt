package dev.forcetower.unes.ui.feature.courseprogress.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.melon.feature.courseprogress.domain.model.CourseProgress
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumStanding
import dev.forcetower.melon.feature.courseprogress.domain.model.CurriculumVersion
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.courseprogress.AUTOMATIC_VERSION_SWITCH
import dev.forcetower.unes.ui.feature.courseprogress.CourseProgressFormat
import dev.forcetower.unes.ui.feature.courseprogress.CourseProgressPreviewData

// The curriculum picker (iOS `CurriculumVersionPicker`): the entry row on the
// progress screen saying which version the numbers are computed on, and the
// sheet listing every version of the course, each scored against the
// student's own history — so "which grid is mine?" is answerable from the
// numbers rather than from memory.

// ───────── Standing vocabulary ─────────

@Composable
private fun CurriculumStanding.shortLabel(): String = stringResource(
    when (this) {
        CurriculumStanding.Current -> R.string.course_progress_version_standing_current
        CurriculumStanding.Previous -> R.string.course_progress_version_standing_previous
        CurriculumStanding.Retired -> R.string.course_progress_version_standing_retired
        CurriculumStanding.Unplaced -> R.string.course_progress_version_standing_unplaced
    },
)

// The grid taking entrants today is "live"; everything else is history.
@Composable
private fun CurriculumStanding.tone(): Color =
    if (this == CurriculumStanding.Current) MaterialTheme.melon.status.ok else MaterialTheme.colorScheme.outline

// "VIGENTE" / "ANTERIOR" — the standing as a capsule tag; the current
// version's is tinted, every other one reads as history.
@Composable
private fun CurriculumStandingChip(standing: CurriculumStanding, modifier: Modifier = Modifier) {
    val tone = standing.tone()
    Text(
        text = standing.shortLabel().uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp,
        ),
        color = tone,
        maxLines = 1,
        modifier = modifier
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.11f))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

// ───────── Entry row on the progress screen ─────────

// Which curriculum the numbers below are computed on, and the door into the
// picker. Only shown when there is something to pick between.
@Composable
internal fun CurriculumVersionButton(
    progress: CourseProgress,
    course: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val curriculum = progress.curriculum
    val standing = curriculum?.let(progress::standing)
    val shape = RoundedCornerShape(16.dp)
    val muted = MaterialTheme.colorScheme.outline

    val title = if (curriculum == null) {
        stringResource(R.string.course_progress_version_pick)
    } else {
        stringResource(R.string.course_progress_version_title_format, curriculum.codeLabel)
    }
    val subtitle = if (standing == null) {
        stringResource(R.string.course_progress_version_none)
    } else {
        listOfNotNull(course?.takeIf { it.isNotBlank() }, standing.shortLabel()).joinToString(" · ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(standing?.tone() ?: muted),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = pluralStringResource(
                R.plurals.course_progress_version_count,
                progress.availableVersions.size,
                progress.availableVersions.size,
            ),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
            color = muted,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = muted,
            modifier = Modifier.size(18.dp),
        )
    }
}

// ───────── Picker sheet ─────────

// Every version of the course, each scored against the student's own history.
// Picking one re-binds server-side and the screen behind re-renders from the
// rebuilt payload. `switchingVersionId` is the version whose switch is in
// flight (or [AUTOMATIC_VERSION_SWITCH]) — the sheet locks and spins on it.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CurriculumVersionPickerSheet(
    progress: CourseProgress,
    course: String?,
    switchingVersionId: String?,
    onPick: (String) -> Unit,
    onAutomatic: () -> Unit,
    onDismiss: () -> Unit,
) {
    val switching = switchingVersionId != null
    ModalBottomSheet(
        // Mid-switch the sheet has to stay up: the request is out and the
        // rebuilt payload lands behind it.
        onDismissRequest = { if (!switching) onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        ) {
            Text(
                text = stringResource(R.string.course_progress_version_sheet_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.6).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (course.isNullOrBlank()) {
                    stringResource(R.string.course_progress_version_sheet_subtitle_fallback)
                } else {
                    stringResource(R.string.course_progress_version_sheet_subtitle_format, course)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                progress.availableVersions.forEach { version ->
                    CurriculumVersionRow(
                        version = version,
                        standing = progress.standing(version),
                        approvedHours = progress.approvedHours,
                        selected = version.id == progress.curriculum?.id,
                        switching = version.id == switchingVersionId,
                        enabled = !switching,
                        onClick = { onPick(version.id) },
                    )
                }
            }

            Text(
                text = stringResource(R.string.course_progress_version_footnote),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 11.dp, start = 2.dp, end = 2.dp),
            )

            if (progress.curriculum?.isManualPick == true) {
                OutlinedButton(
                    onClick = onAutomatic,
                    enabled = !switching,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    if (switchingVersionId == AUTOMATIC_VERSION_SWITCH) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(14.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.course_progress_version_automatic),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                    )
                }
            }
        }
    }
}

// One version: code, its place in the succession, the student's numbers
// against it, and how much of their history it accounts for.
@Composable
private fun CurriculumVersionRow(
    version: CurriculumVersion,
    standing: CurriculumStanding,
    // The student's whole pool of passed hours — what `fit` is a share of.
    approvedHours: Int,
    selected: Boolean,
    switching: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val ok = MaterialTheme.melon.status.ok
    val shape = RoundedCornerShape(18.dp)
    val completed = version.completedHours ?: 0
    val required = version.requiredHours

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.melon.surface.line,
                shape = shape,
            )
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 13.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.course_progress_version_title_format, version.codeLabel),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            CurriculumStandingChip(standing = standing)
            Spacer(Modifier.weight(1f))
            when {
                switching -> CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
                selected -> Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = stringResource(R.string.course_progress_version_selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Text(
            text = versionHint(version, standing),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 3.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (required == null) {
                    stringResource(
                        R.string.course_progress_version_hours_only_format,
                        stringResource(R.string.course_progress_hours_format, CourseProgressFormat.count(completed)),
                    )
                } else {
                    stringResource(
                        R.string.course_progress_hours_of_required_format,
                        CourseProgressFormat.count(completed),
                        CourseProgressFormat.count(required),
                    )
                },
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            val percent = version.percent
            if (percent != null && required != null) {
                Text(
                    text = CourseProgressFormat.headlinePercent(percent),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }

        Box(modifier = Modifier.padding(top = 7.dp)) {
            if (required == null) {
                HatchedTrack(height = 6.dp)
            } else {
                val fraction = if (required > 0) completed.toFloat() / required else 0f
                CurriculumMeter(
                    fraction = fraction.coerceIn(0f, 1f),
                    tone = if (selected) ok else ok.copy(alpha = 0.45f),
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Sync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = version.fit?.let { fit ->
                    stringResource(
                        R.string.course_progress_version_fit_format,
                        CourseProgressFormat.percent(fit, fractionDigits = 0),
                        stringResource(R.string.course_progress_hours_format, CourseProgressFormat.count(approvedHours)),
                    )
                } ?: stringResource(R.string.course_progress_version_fit_unknown),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.sp,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

// A thin filled track — the row's own gauge, tinted down on the versions the
// student is not on.
@Composable
private fun CurriculumMeter(fraction: Float, tone: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(tone.copy(alpha = 0.18f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(6.dp)
                .clip(CircleShape)
                .background(tone),
        )
    }
}

@Composable
private fun versionHint(version: CurriculumVersion, standing: CurriculumStanding): String = when (standing) {
    CurriculumStanding.Current -> stringResource(R.string.course_progress_version_hint_current)
    CurriculumStanding.Previous, CurriculumStanding.Retired -> {
        val successor = version.supersededBy
        val effective = successor?.effectiveFromLabel
        when {
            successor == null -> stringResource(R.string.course_progress_version_hint_unplaced)
            effective != null -> stringResource(
                R.string.course_progress_version_hint_superseded_format,
                successor.codeLabel,
                effective,
            )
            else -> stringResource(
                R.string.course_progress_version_hint_superseded_undated_format,
                successor.codeLabel,
            )
        }
    }
    CurriculumStanding.Unplaced -> stringResource(R.string.course_progress_version_hint_unplaced)
}

// ───────── Previews ─────────

@Preview
@Composable
private fun CurriculumVersionButtonPreview() {
    MelonTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CurriculumVersionButton(
                progress = CourseProgressPreviewData.progress,
                course = "Psicologia",
                onClick = {},
            )
            CurriculumVersionButton(
                progress = CourseProgressPreviewData.progress.copy(curriculum = null),
                course = "Psicologia",
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun CurriculumVersionRowsPreview() {
    MelonTheme {
        val progress = CourseProgressPreviewData.progress
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            progress.availableVersions.forEach { version ->
                CurriculumVersionRow(
                    version = version,
                    standing = progress.standing(version),
                    approvedHours = progress.approvedHours,
                    selected = version.id == progress.curriculum?.id,
                    switching = false,
                    enabled = true,
                    onClick = {},
                )
            }
        }
    }
}
