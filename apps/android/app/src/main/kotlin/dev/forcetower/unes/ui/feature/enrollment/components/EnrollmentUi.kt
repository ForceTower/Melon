package dev.forcetower.unes.ui.feature.enrollment.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon

// Shared chrome for the matrícula flow: the pushed-screen app bar, tinted
// notice banners and the status/success stat tiles (dc `MatriculaScreen`).

@Composable
internal fun EnrollmentAppBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backLabel = stringResource(R.string.enrollment_back)
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
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(44.dp))
    }
}

internal enum class EnrollmentBannerTone { Danger, Warn, Ok, Neutral }

// Icon-in-a-dot notice banner (prereq status, timetable summary, review
// blockers, comprovante notice).
@Composable
internal fun EnrollmentBanner(
    tone: EnrollmentBannerTone,
    icon: ImageVector,
    title: String,
    text: String,
    modifier: Modifier = Modifier,
) {
    val hue = when (tone) {
        EnrollmentBannerTone.Danger -> MaterialTheme.melon.status.bad
        EnrollmentBannerTone.Warn -> MaterialTheme.melon.status.warn
        EnrollmentBannerTone.Ok -> MaterialTheme.melon.status.ok
        EnrollmentBannerTone.Neutral -> MaterialTheme.colorScheme.outline
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(hue.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(hue),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(14.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.5.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

// Tonal stat tile — the disciplinas/conflitos/em fila trio on the status hub.
@Composable
internal fun EnrollmentStatTile(
    label: String,
    value: String,
    hint: String,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onBackground,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 13.dp, vertical = 14.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.1).sp,
            ),
            color = valueColor,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

// Small tinted tag pill — Sugerida / Pré-requisito / Fila markers.
@Composable
internal fun EnrollmentTagPill(
    text: String,
    hue: Color,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    solid: Boolean = false,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(if (solid) hue else hue.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (solid) MaterialTheme.melon.fixed.onHero else hue,
                modifier = Modifier.size(12.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (solid) MaterialTheme.melon.fixed.onHero else hue),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = if (solid) MaterialTheme.melon.fixed.onHero else hue,
        )
    }
}

// Discipline code chip tinted by the stable per-code hue.
@Composable
internal fun EnrollmentCodeChip(code: String, hue: Color, modifier: Modifier = Modifier) {
    Text(
        text = code,
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.3.sp,
        ),
        color = hue,
        modifier = modifier
            .clip(RoundedCornerShape(7.dp))
            .background(hue.copy(alpha = 0.13f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}
