package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Bottom action dock shared by every matrícula step (dc `MatriculaScreen` M3
// bottom bar): hour readout + context line on the left, optional save/grade
// affordances and the step's primary action on the right, with an optional
// blocker strip above.
@Composable
internal fun EnrollmentDock(
    totalHours: Int,
    maxHours: Int,
    hoursColor: Color,
    subText: String,
    subColor: Color,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    primaryEnabled: Boolean = true,
    primaryFillsWidth: Boolean = false,
    submitting: Boolean = false,
    blockerText: String? = null,
    onSave: (() -> Unit)? = null,
    onGrade: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    secondaryIcon: ImageVector? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val line = MaterialTheme.melon.surface.line
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
            .drawBehind {
                drawLine(
                    color = line,
                    start = Offset.Zero,
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        if (blockerText != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.melon.status.bad),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PriorityHigh,
                        contentDescription = null,
                        tint = MaterialTheme.melon.fixed.onHero,
                        modifier = Modifier.size(11.dp),
                    )
                }
                Text(
                    text = blockerText,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.melon.status.bad,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.enrollment_hours_format, totalHours),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 21.sp,
                            lineHeight = 21.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.7).sp,
                        ),
                        color = hoursColor,
                    )
                    Text(
                        text = stringResource(R.string.enrollment_dock_max_format, maxHours),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.padding(bottom = 1.dp),
                    )
                }
                Text(
                    text = subText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = subColor,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (!primaryFillsWidth) {
                Box(modifier = Modifier.weight(1f))
            }
            if (onSave != null) {
                DockSquareButton(
                    icon = Icons.Filled.BookmarkAdd,
                    contentDescription = stringResource(R.string.enrollment_dock_save),
                    onClick = onSave,
                )
            }
            if (onGrade != null) {
                DockOutlinedButton(
                    icon = Icons.Filled.CalendarViewWeek,
                    label = stringResource(R.string.enrollment_dock_grade),
                    onClick = onGrade,
                )
            }
            if (secondaryLabel != null && secondaryIcon != null && onSecondary != null) {
                DockOutlinedButton(
                    icon = secondaryIcon,
                    label = secondaryLabel,
                    onClick = onSecondary,
                )
            }
            Button(
                onClick = onPrimary,
                // Stays accent-tinted while the spinner shows — the ViewModel
                // already ignores re-taps mid-submit.
                enabled = primaryEnabled || submitting,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp),
                modifier = (if (primaryFillsWidth) Modifier.weight(1f) else Modifier).height(48.dp),
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = stringResource(R.string.enrollment_dock_submitting),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                } else {
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.15).sp,
                        ),
                    )
                    if (primaryIcon != null) {
                        Icon(
                            imageVector = primaryIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 7.dp)
                                .size(19.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockSquareButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun DockOutlinedButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(15.dp)
    Row(
        modifier = Modifier
            .height(48.dp)
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
