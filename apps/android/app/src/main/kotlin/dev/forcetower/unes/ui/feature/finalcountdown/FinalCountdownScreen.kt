package dev.forcetower.unes.ui.feature.finalcountdown

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
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCComposition
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCDisciplineRow
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCDisciplineSelectorSheet
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCGradeList
import dev.forcetower.unes.ui.feature.finalcountdown.components.FCVerdictHero

// "Final Countdown" — the approval calculator, rebuilt to the dc
// `FinalCountdownScreen` spec: discipline row + selector sheet, mesh verdict
// hero with the drawn average ring, composição bars, weighted-mode switch,
// fully editable evaluation list, reset, and the rules info card. Everything
// recomputes live via `FinalCountdownMath`.
@Composable
internal fun FinalCountdownScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    offerId: String? = null,
    bottomInset: Dp = 0.dp,
) {
    val vm: FinalCountdownViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(offerId) {
        vm.onIntent(FinalCountdownIntent.SeedFromRoute(offerId))
    }
    FinalCountdownContent(
        state = state,
        onBack = onBack,
        onIntent = vm::onIntent,
        modifier = modifier,
        bottomInset = bottomInset,
    )
}

@Composable
private fun FinalCountdownContent(
    state: FinalCountdownUiState,
    onBack: () -> Unit,
    onIntent: (FinalCountdownIntent) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    var selectorOpen by rememberSaveable { mutableStateOf(false) }
    val verdict = remember(state.rows, state.weighted) {
        FinalCountdownMath.verdict(rows = state.rows, weighted = state.weighted)
    }
    val style = fcVerdictStyle(verdict, state.nextSemesterLabel)

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // The app bar stays pinned; the headline and the calculator scroll
    // beneath it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        FCAppBar(
            onBack = onBack,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .fadeUpOnAppear(delayMs = 20),
        )
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset),
        ) {
            FCHeadline(modifier = Modifier.fadeUpOnAppear(delayMs = 40))

            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)) {
                FCDisciplineRow(
                    discipline = state.discipline,
                    onChange = { selectorOpen = true },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 80),
                )

                FCVerdictHero(
                    verdict = verdict,
                    style = style,
                    weighted = state.weighted,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fadeUpOnAppear(delayMs = 140),
                )

                FCComposition(
                    rows = state.rows,
                    weighted = state.weighted,
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fadeUpOnAppear(delayMs = 240),
                )

                EvaluationsHeader(
                    filled = state.rows.count { it.score != null },
                    total = state.rows.size,
                    modifier = Modifier
                        .padding(start = 4.dp, end = 4.dp, top = 20.dp, bottom = 12.dp)
                        .fadeUpOnAppear(delayMs = 300),
                )

                WeightedToggleCard(
                    weighted = state.weighted,
                    onToggle = { onIntent(FinalCountdownIntent.ToggleWeighted) },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 340),
                )

                FCGradeList(
                    rows = state.rows,
                    weighted = state.weighted,
                    onIntent = onIntent,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fadeUpOnAppear(delayMs = 380),
                )

                ResetButton(
                    onClick = { onIntent(FinalCountdownIntent.Reset) },
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fadeUpOnAppear(delayMs = 420),
                )

                InfoCard(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .fadeUpOnAppear(delayMs = 460),
                )

                Text(
                    text = stringResource(R.string.final_countdown_footer),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 4.dp),
                )

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    if (selectorOpen) {
        FCDisciplineSelectorSheet(
            choices = state.choices,
            activeOfferId = state.discipline?.offerId,
            onPick = { offerId ->
                onIntent(FinalCountdownIntent.PickDiscipline(offerId))
                selectorOpen = false
            },
            onDismiss = { selectorOpen = false },
        )
    }
}

@Composable
private fun FCAppBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val backLabel = stringResource(R.string.final_countdown_back)
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
            text = stringResource(R.string.final_countdown_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.17).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        Spacer(modifier = Modifier.width(44.dp))
    }
}

@Composable
private fun FCHeadline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.final_countdown_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 30.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.final_countdown_headline_body),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(top = 7.dp)
                .widthIn(max = 300.dp),
        )
    }
}

@Composable
private fun EvaluationsHeader(filled: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.final_countdown_evaluations_title),
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.4).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.final_countdown_filled_format, filled, total),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(bottom = 2.dp),
        )
    }
}

@Composable
private fun WeightedToggleCard(
    weighted: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.melon.palette.jade),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Balance,
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.final_countdown_weighted_title),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(
                    if (weighted) R.string.final_countdown_weighted_hint_on
                    else R.string.final_countdown_weighted_hint_off,
                ),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Switch(checked = weighted, onCheckedChange = { onToggle() })
    }
}

@Composable
private fun ResetButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    val label = stringResource(R.string.final_countdown_clear)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.15).sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun InfoCard(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val warn = MaterialTheme.melon.status.warn
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(warn.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = warn,
                modifier = Modifier.size(17.dp),
            )
        }
        val bold = SpanStyle(color = ink, fontWeight = FontWeight.Bold)
        val text = buildAnnotatedString {
            append(stringResource(R.string.final_countdown_info_intro))
            withStyle(bold) { append(stringResource(R.string.final_countdown_info_pass_band)) }
            append(stringResource(R.string.final_countdown_info_final_intro))
            withStyle(bold) { append(stringResource(R.string.final_countdown_info_final_band)) }
            append(stringResource(R.string.final_countdown_info_fail_intro))
            withStyle(bold) { append(stringResource(R.string.final_countdown_info_fail_band)) }
            append(stringResource(R.string.final_countdown_info_outro))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 18.5.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Preview
@Composable
private fun FinalCountdownPreview() {
    MelonTheme {
        FinalCountdownContent(
            state = FinalCountdownUiState(
                rows = listOf(
                    FCRow(label = "VA1", scoreText = "6,5"),
                    FCRow(label = "VA2", scoreText = "5,2"),
                    FCRow(label = "Trab"),
                ),
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
