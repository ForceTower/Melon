package dev.forcetower.unes.ui.feature.onboarding.intro

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.MelonPaletteColors
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.onboarding.components.OnboardingPillButton
import dev.forcetower.unes.ui.feature.onboarding.intro.illustrations.GradesIllustration
import dev.forcetower.unes.ui.feature.onboarding.intro.illustrations.MessagesIllustration
import dev.forcetower.unes.ui.feature.onboarding.intro.illustrations.NotificationsIllustration
import dev.forcetower.unes.ui.feature.onboarding.intro.illustrations.ScheduleIllustration
import kotlinx.coroutines.launch

private enum class IntroSlide(
    val variant: MeshVariant,
    @StringRes val eyebrowRes: Int,
    @StringRes val headlineTopRes: Int,
    @StringRes val headlineAccentRes: Int,
    @StringRes val bodyRes: Int,
) {
    Schedule(
        variant = MeshVariant.Cool,
        eyebrowRes = R.string.onboarding_intro_schedule_eyebrow,
        headlineTopRes = R.string.onboarding_intro_schedule_headline_top,
        headlineAccentRes = R.string.onboarding_intro_schedule_headline_accent,
        bodyRes = R.string.onboarding_intro_schedule_body,
    ),
    Grades(
        variant = MeshVariant.Sun,
        eyebrowRes = R.string.onboarding_intro_grades_eyebrow,
        headlineTopRes = R.string.onboarding_intro_grades_headline_top,
        headlineAccentRes = R.string.onboarding_intro_grades_headline_accent,
        bodyRes = R.string.onboarding_intro_grades_body,
    ),
    Messages(
        variant = MeshVariant.Rose,
        eyebrowRes = R.string.onboarding_intro_messages_eyebrow,
        headlineTopRes = R.string.onboarding_intro_messages_headline_top,
        headlineAccentRes = R.string.onboarding_intro_messages_headline_accent,
        bodyRes = R.string.onboarding_intro_messages_body,
    ),
    Notifications(
        variant = MeshVariant.Warm,
        eyebrowRes = R.string.onboarding_intro_notifications_eyebrow,
        headlineTopRes = R.string.onboarding_intro_notifications_headline_top,
        headlineAccentRes = R.string.onboarding_intro_notifications_headline_accent,
        bodyRes = R.string.onboarding_intro_notifications_body,
    ),
}

// Per-slide accent — tints the eyebrow and the headline's second line
// (dc slide `accent`). Notifications uses the theme accent.
@Composable
private fun IntroSlide.accent(palette: MelonPaletteColors): Color = when (this) {
    IntroSlide.Schedule -> palette.teal
    IntroSlide.Grades -> palette.orange
    IntroSlide.Messages -> palette.magenta
    IntroSlide.Notifications -> MaterialTheme.colorScheme.primary
}

@Composable
fun IntroCarouselScreen(
    onDone: () -> Unit,
    onBack: () -> Unit,
    requestNotifications: () -> Unit = rememberRequestNotificationPermission(),
) {
    val slides = remember { IntroSlide.entries.toList() }
    val pagerState = rememberPagerState { slides.size }
    val scope = rememberCoroutineScope()
    // iOS prompts for notifications on the final-slide CTA (and skip), then
    // immediately navigates regardless of the user's choice.
    val finishWithPermissionPrompt = {
        requestNotifications()
        onDone()
    }

    val pageBg = MaterialTheme.colorScheme.background
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        Modifier
            .fillMaxSize()
            .background(pageBg),
    ) {
        // Top bar — back / dots / skip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 58.dp, start = 18.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val backLabel = stringResource(R.string.onboarding_intro_back)
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = backLabel,
                    ) {
                        if (pagerState.currentPage > 0) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                        } else {
                            onBack()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = backLabel,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Dots are visual progress only; merge into a single
            // page-position announcement instead of N separate nodes.
            val pageDescription = stringResource(
                R.string.onboarding_intro_page_position,
                pagerState.currentPage + 1,
                slides.size,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics(mergeDescendants = true) {
                    contentDescription = pageDescription
                },
            ) {
                slides.forEachIndexed { i, _ ->
                    val active = i == pagerState.currentPage
                    val width by animateDpAsState(
                        targetValue = if (active) 22.dp else 6.dp,
                        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
                        label = "dot-w",
                    )
                    Box(
                        Modifier
                            .width(width)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                },
                            ),
                    )
                }
            }

            Text(
                text = stringResource(R.string.onboarding_intro_skip),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = ink3,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.onboarding_intro_skip_label),
                    ) { finishWithPermissionPrompt() }
                    .padding(8.dp),
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            SlidePage(slide = slides[page])
        }

        OnboardingPillButton(
            text = if (pagerState.currentPage == slides.lastIndex) {
                stringResource(R.string.onboarding_intro_finish)
            } else {
                stringResource(R.string.onboarding_intro_continue)
            },
            onClick = {
                if (pagerState.currentPage < slides.lastIndex) {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                } else {
                    finishWithPermissionPrompt()
                }
            },
            showArrow = true,
            arrowIcon = Icons.AutoMirrored.Filled.ArrowForward,
            modifier = Modifier.padding(start = 28.dp, end = 28.dp, bottom = 46.dp),
        )
    }
}

@Composable
private fun SlidePage(slide: IntroSlide) {
    val accent = slide.accent(MaterialTheme.melon.palette)
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier.fillMaxSize()) {
        // Illustration stage — inset mesh panel with the slide art on top.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(340.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(26.dp)
                    .scaleInOnAppear(durationMs = 500, fromScale = 0.96f)
                    .clip(RoundedCornerShape(34.dp)),
            ) {
                Mesh(
                    variant = slide.variant,
                    intensity = 0.65f,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.83f)),
                )
            }
            when (slide) {
                IntroSlide.Schedule -> ScheduleIllustration()
                IntroSlide.Grades -> GradesIllustration()
                IntroSlide.Messages -> MessagesIllustration()
                IntroSlide.Notifications -> NotificationsIllustration()
            }
        }

        Spacer(Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 28.dp, end = 28.dp, bottom = 26.dp),
        ) {
            Text(
                text = stringResource(slide.eyebrowRes).uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.78.sp,
                ),
                color = accent,
                modifier = Modifier.fadeUpOnAppear(delayMs = 80, durationMs = 500),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = slideHeadline(slide, ink = ink, accent = accent),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 42.sp,
                    lineHeight = 43.sp,
                    letterSpacing = (-1.7).sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                modifier = Modifier.fadeUpOnAppear(delayMs = 160),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(slide.bodyRes),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.08).sp,
                ),
                color = ink3,
                modifier = Modifier.fadeUpOnAppear(delayMs = 260),
            )
        }
    }
}

@Composable
private fun slideHeadline(slide: IntroSlide, ink: Color, accent: Color): AnnotatedString {
    val top = stringResource(slide.headlineTopRes)
    val second = stringResource(slide.headlineAccentRes)
    return buildAnnotatedString {
        withStyle(SpanStyle(color = ink)) { append("$top\n") }
        withStyle(SpanStyle(color = accent)) { append(second) }
    }
}

@Preview
@Composable
private fun IntroCarouselScreenPreview() {
    MelonTheme {
        IntroCarouselScreen(
            onDone = {},
            onBack = {},
            requestNotifications = {},
        )
    }
}
