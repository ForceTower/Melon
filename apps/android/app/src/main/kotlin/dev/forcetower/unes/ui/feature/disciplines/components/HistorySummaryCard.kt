package dev.forcetower.unes.ui.feature.disciplines.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import java.util.Locale

// "Desempenho acumulado" card at the top of the Histórico tab — overall CR,
// count of disciplines taken, and approval rate across downloaded semesters.
// Every number belongs to one program: a student with a mestrado on top of
// their graduação picks which one through the chip row, and students with a
// single program never see it.
@Composable
internal fun HistorySummaryCard(
    overallMean: Double?,
    taken: Int,
    approvalPercent: Int?,
    modifier: Modifier = Modifier,
    programTracks: List<String?> = emptyList(),
    selectedTrack: String? = null,
    onSelectProgram: (String?) -> Unit = {},
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(18.dp),
    ) {
        Text(
            text = stringResource(R.string.disciplines_history_performance).uppercase(Locale.ROOT),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
        if (programTracks.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            ProgramChips(
                tracks = programTracks,
                selectedTrack = selectedTrack,
                onSelectProgram = onSelectProgram,
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                value = formatGrade(overallMean),
                label = stringResource(R.string.disciplines_history_overall_mean),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = taken.toString(),
                label = stringResource(R.string.disciplines_history_courses_taken),
                valueColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                value = approvalPercent?.let { "$it%" } ?: "–",
                label = stringResource(R.string.disciplines_history_approval_rate),
                valueColor = MaterialTheme.melon.status.ok,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

// One chip per program the student has semesters in. The track code is the
// label — upstream gives us no name for a program, only the suffix it stamps
// on its semester codes (22.1RUE → "RUE").
@Composable
private fun ProgramChips(
    tracks: List<String?>,
    selectedTrack: String?,
    onSelectProgram: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tracks.forEach { track ->
            val selected = track == selectedTrack
            FilterChip(
                selected = selected,
                onClick = { onSelectProgram(track) },
                modifier = Modifier.height(32.dp),
                shape = RoundedCornerShape(9.dp),
                label = {
                    Text(
                        text = track ?: stringResource(R.string.program_undergrad),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                        ),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    borderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.18f),
                ),
            )
        }
    }
}

@Composable
private fun StatTile(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.52).sp,
            ),
            color = valueColor,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview
@Composable
private fun HistorySummaryCardPreview() {
    MelonTheme {
        HistorySummaryCard(
            overallMean = 7.0,
            taken = 16,
            approvalPercent = 88,
            programTracks = listOf(null, "RUE"),
            selectedTrack = null,
        )
    }
}
