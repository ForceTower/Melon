package dev.forcetower.unes.ui.feature.onboarding.intro.illustrations

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeInOnAppear
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonMotion
import dev.forcetower.unes.designsystem.theme.melon
import kotlinx.coroutines.delay

// Slide illustrations for the intro carousel (dc `UNES Onboarding - Android`).
// All mock content lives in strings.xml; colors come from the theme.

// ──────────────── 1 · Horário (mini timetable) ────────────────

private val ScheduleRowHeight = 50.dp
private val ScheduleGap = 6.dp

@Composable
internal fun ScheduleIllustration() {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val brand = MaterialTheme.melon.brand

    Column(Modifier.width(250.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
            horizontalArrangement = Arrangement.spacedBy(ScheduleGap),
        ) {
            Spacer(Modifier.width(34.dp))
            listOf(
                R.string.onboarding_illu_sched_day_1,
                R.string.onboarding_illu_sched_day_2,
                R.string.onboarding_illu_sched_day_3,
            ).forEach { day ->
                Text(
                    text = stringResource(day),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.9.sp,
                    ),
                    color = ink4,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(ScheduleGap)) {
            Column(
                modifier = Modifier
                    .width(34.dp)
                    .height(ScheduleRowHeight * 4 + ScheduleGap * 3)
                    .padding(end = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End,
            ) {
                listOf(
                    R.string.onboarding_illu_sched_hour_1,
                    R.string.onboarding_illu_sched_hour_2,
                    R.string.onboarding_illu_sched_hour_3,
                    R.string.onboarding_illu_sched_hour_4,
                ).forEach { hour ->
                    Text(
                        text = stringResource(hour),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = ink4,
                    )
                }
            }
            // seg
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScheduleGap)) {
                ScheduleBlock(
                    color = brand.coral,
                    codeRes = R.string.onboarding_illu_sched_code_algi,
                    roomRes = R.string.onboarding_illu_sched_room_214,
                    height = ScheduleRowHeight * 2 + ScheduleGap,
                    delayMs = 100,
                )
                Spacer(Modifier.height(ScheduleRowHeight))
                ScheduleBlock(
                    color = brand.amber,
                    codeRes = R.string.onboarding_illu_sched_code_proj,
                    roomRes = null,
                    height = ScheduleRowHeight,
                    delayMs = 580,
                )
            }
            // ter
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScheduleGap)) {
                Spacer(Modifier.height(ScheduleRowHeight))
                ScheduleBlock(
                    color = brand.amber,
                    codeRes = R.string.onboarding_illu_sched_code_calc,
                    roomRes = null,
                    height = ScheduleRowHeight,
                    delayMs = 340,
                )
            }
            // qua
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ScheduleGap)) {
                ScheduleBlock(
                    color = brand.magenta,
                    codeRes = R.string.onboarding_illu_sched_code_lpoo,
                    roomRes = null,
                    height = ScheduleRowHeight,
                    delayMs = 220,
                )
                Spacer(Modifier.height(ScheduleRowHeight))
                ScheduleBlock(
                    color = brand.plum,
                    codeRes = R.string.onboarding_illu_sched_code_fis2,
                    roomRes = R.string.onboarding_illu_sched_room_312,
                    height = ScheduleRowHeight * 2 + ScheduleGap,
                    delayMs = 460,
                )
            }
        }
    }
}

@Composable
private fun ScheduleBlock(
    color: Color,
    @StringRes codeRes: Int,
    @StringRes roomRes: Int?,
    height: Dp,
    delayMs: Int,
) {
    val onHero = MaterialTheme.melon.fixed.onHero
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fadeUpOnAppear(delayMs = delayMs, durationMs = 500)
            .shadow(4.dp, RoundedCornerShape(11.dp), spotColor = color)
            .height(height)
            .clip(RoundedCornerShape(11.dp))
            .background(color)
            .padding(9.dp),
    ) {
        Text(
            text = stringResource(codeRes),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = onHero,
        )
        if (roomRes != null) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(roomRes),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = onHero.copy(alpha = 0.85f),
            )
        }
    }
}

// ──────────────── 2 · Notas (coefficient + bars) ────────────────

private data class GradeBar(val labelRes: Int, val height: Dp)

@Composable
internal fun GradesIllustration() {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val brand = MaterialTheme.melon.brand
    val bars = listOf(
        GradeBar(R.string.onboarding_illu_grades_bar_1, 58.dp) to brand.amber,
        GradeBar(R.string.onboarding_illu_grades_bar_2, 76.dp) to brand.coral,
        GradeBar(R.string.onboarding_illu_grades_bar_3, 92.dp) to brand.magenta,
        GradeBar(R.string.onboarding_illu_grades_bar_4, 66.dp) to brand.amber,
        GradeBar(R.string.onboarding_illu_grades_bar_5, 84.dp) to brand.coral,
    )

    Column(Modifier.width(250.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        val value = stringResource(R.string.onboarding_illu_grades_value)
        val scale = stringResource(R.string.onboarding_illu_grades_scale)
        Text(
            text = buildAnnotatedString {
                value.forEach { char ->
                    if (char == ',') {
                        withStyle(SpanStyle(color = accent)) { append(char) }
                    } else {
                        append(char)
                    }
                }
                withStyle(
                    SpanStyle(
                        fontSize = 26.sp,
                        color = ink3,
                        fontWeight = FontWeight.Bold,
                    ),
                ) { append(scale) }
            },
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 88.sp,
                lineHeight = 88.sp,
                letterSpacing = (-3.5).sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = ink,
            modifier = Modifier.fadeUpOnAppear(delayMs = 100),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.onboarding_illu_grades_caption).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
            ),
            color = ink3,
            modifier = Modifier.fadeInOnAppear(delayMs = 300, durationMs = 800),
        )
        Spacer(Modifier.height(28.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.height(120.dp),
        ) {
            bars.forEachIndexed { index, (bar, color) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(bar.labelRes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        color = ink3,
                        modifier = Modifier.fadeInOnAppear(delayMs = 700 + index * 100, durationMs = 400),
                    )
                    Spacer(Modifier.height(7.dp))
                    GrowingBar(color = color, targetHeight = bar.height, delayMs = 340 + index * 80)
                }
            }
        }
    }
}

@Composable
private fun GrowingBar(color: Color, targetHeight: Dp, delayMs: Int) {
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs.toLong())
        grown = true
    }
    val height by animateDpAsState(
        targetValue = if (grown) targetHeight else 0.dp,
        animationSpec = tween(700, easing = MelonMotion.PopEasing),
        label = "bar-grow",
    )
    Box(
        Modifier
            .width(26.dp)
            .height(height)
            .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(color),
    )
}

// ──────────────── 3 · Recados (inbox cards) ────────────────

@Composable
internal fun MessagesIllustration() {
    val brand = MaterialTheme.melon.brand
    Column(Modifier.width(258.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        MessageCard(
            avatarColor = brand.magenta,
            nameRes = R.string.onboarding_illu_msg_1_name,
            timeRes = R.string.onboarding_illu_msg_1_time,
            previewRes = R.string.onboarding_illu_msg_1_preview,
            rotation = -1.5f,
            unread = true,
            delayMs = 100,
        )
        MessageCard(
            avatarColor = brand.coral,
            nameRes = R.string.onboarding_illu_msg_2_name,
            timeRes = R.string.onboarding_illu_msg_2_time,
            previewRes = R.string.onboarding_illu_msg_2_preview,
            rotation = 1.5f,
            unread = false,
            delayMs = 250,
        )
        MessageCard(
            avatarColor = brand.amber,
            nameRes = R.string.onboarding_illu_msg_3_name,
            timeRes = R.string.onboarding_illu_msg_3_time,
            previewRes = R.string.onboarding_illu_msg_3_preview,
            rotation = -1f,
            unread = false,
            delayMs = 400,
        )
    }
}

@Composable
private fun MessageCard(
    avatarColor: Color,
    @StringRes nameRes: Int,
    @StringRes timeRes: Int,
    @StringRes previewRes: Int,
    rotation: Float,
    unread: Boolean,
    delayMs: Int,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary
    val onHero = MaterialTheme.melon.fixed.onHero
    val name = stringResource(nameRes)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotation)
            .fadeUpOnAppear(delayMs = delayMs, durationMs = 600, fromOffset = 18.dp)
            .shadow(6.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.first().uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = onHero,
            )
        }
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = ink,
                )
                Text(
                    text = stringResource(timeRes),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = ink4,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(previewRes),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                color = ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (unread) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
        }
    }
}

// ──────────────── 4 · Notificações (lockscreen stack) ────────────────

@Composable
internal fun NotificationsIllustration() {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val brand = MaterialTheme.melon.brand

    Column(Modifier.width(258.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fadeInOnAppear(delayMs = 50, durationMs = 500),
        ) {
            val clock = stringResource(R.string.onboarding_illu_notif_clock)
            Text(
                text = buildAnnotatedString {
                    clock.forEach { char ->
                        if (char == ':') {
                            withStyle(SpanStyle(color = ink.copy(alpha = 0.4f))) { append(char) }
                        } else {
                            append(char)
                        }
                    }
                },
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 34.sp,
                    letterSpacing = (-1).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = ink,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.onboarding_illu_notif_date).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.8.sp,
                ),
                color = ink3,
            )
        }
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            NotificationCard(
                icon = Icons.Filled.Grade,
                iconTint = brand.amber,
                categoryRes = R.string.onboarding_illu_notif_1_category,
                titleRes = R.string.onboarding_illu_notif_1_title,
                bodyRes = R.string.onboarding_illu_notif_1_body,
                delayMs = 150,
            )
            NotificationCard(
                icon = Icons.Filled.Forum,
                iconTint = brand.magenta,
                categoryRes = R.string.onboarding_illu_notif_2_category,
                titleRes = R.string.onboarding_illu_notif_2_title,
                bodyRes = R.string.onboarding_illu_notif_2_body,
                delayMs = 280,
            )
            NotificationCard(
                icon = Icons.Filled.Schedule,
                iconTint = brand.coral,
                categoryRes = R.string.onboarding_illu_notif_3_category,
                titleRes = R.string.onboarding_illu_notif_3_title,
                bodyRes = R.string.onboarding_illu_notif_3_body,
                delayMs = 410,
            )
        }
    }
}

@Composable
private fun NotificationCard(
    icon: ImageVector,
    iconTint: Color,
    @StringRes categoryRes: Int,
    @StringRes titleRes: Int,
    @StringRes bodyRes: Int,
    delayMs: Int,
) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val onHero = MaterialTheme.melon.fixed.onHero

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .fadeUpOnAppear(delayMs = delayMs, durationMs = 550, fromOffset = 18.dp)
            .shadow(8.dp, RoundedCornerShape(15.dp))
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(15.dp))
            .padding(horizontal = 11.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = onHero,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(categoryRes).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                ),
                color = ink3,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = ink,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = stringResource(bodyRes),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = ink3,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
