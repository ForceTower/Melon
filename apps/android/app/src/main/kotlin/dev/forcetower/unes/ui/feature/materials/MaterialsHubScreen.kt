package dev.forcetower.unes.ui.feature.materials

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import dev.forcetower.melon.feature.materials.domain.model.MaterialsOverview
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.foundation.scaleInOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.materials.components.DisciplineCodeTile
import dev.forcetower.unes.ui.feature.materials.components.MaterialsBackButton
import dev.forcetower.unes.ui.feature.materials.components.MaterialsCard
import dev.forcetower.unes.ui.feature.materials.components.MaterialsHeroBadge
import dev.forcetower.unes.ui.feature.materials.components.MaterialsHeroPlate
import dev.forcetower.unes.ui.feature.materials.components.MaterialsSectionLabel
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadEffect
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadIntent
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadSheet
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadViewModel
import dev.forcetower.unes.ui.feature.overview.ColorFor

// Materiais hub (dc `MateriaisScreen` hub state): mesh hero pitching the
// collaborative shelf + one card per current-semester discipline with its
// per-type tallies. Pushed from the "Materiais" shortcut on the Me hub.
@Composable
internal fun MaterialsHubScreen(
    onBack: () -> Unit,
    onOpenDiscipline: (MaterialsDiscipline) -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: MaterialsHubViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val uploadVm: MaterialsUploadViewModel = hiltViewModel()

    LaunchedEffect(Unit) { vm.onIntent(MaterialsHubIntent.Load) }
    uploadVm.effects.collectAsEffect { effect ->
        if (effect is MaterialsUploadEffect.Finished) vm.onIntent(MaterialsHubIntent.Load)
    }

    val overview = state.overview

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // The app bar stays pinned; the headline and the shelf scroll beneath it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp)
                .height(48.dp),
        ) {
            MaterialsBackButton(onBack = onBack)
        }
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 24.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = stringResource(R.string.materials_title),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 32.sp,
                        letterSpacing = (-0.64).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
                if (overview != null) {
                    Text(
                        text = stringResource(
                            R.string.materials_hub_subtitle,
                            overview.totalCount,
                            overview.disciplines.size,
                            overview.semester,
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(top = 7.dp)
                            .fadeUpOnAppear(delayMs = 60),
                    )
                }
            }

            when {
                overview != null -> HubContent(
                    overview = overview,
                    onOpenDiscipline = {
                        vm.trackDisciplineOpen(it.id)
                        onOpenDiscipline(it)
                    },
                    onOpenSaved = {
                        vm.trackSavedOpen()
                        onOpenSaved()
                    },
                    onContribute = {
                        uploadVm.onIntent(MaterialsUploadIntent.StartFromHub(overview.disciplines))
                    },
                )
                state.loadFailed -> HubError(onRetry = { vm.onIntent(MaterialsHubIntent.Retry) })
                else -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }

    MaterialsUploadSheet(vm = uploadVm)
}

@Composable
private fun HubContent(
    overview: MaterialsOverview,
    onOpenDiscipline: (MaterialsDiscipline) -> Unit,
    onOpenSaved: () -> Unit,
    onContribute: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(16.dp))
        HubHero(
            savedCount = overview.savedCount,
            onContribute = onContribute,
            onOpenSaved = onOpenSaved,
            modifier = Modifier.scaleInOnAppear(delayMs = 100, fromScale = 0.97f),
        )

        Spacer(Modifier.height(26.dp))
        MaterialsSectionLabel(
            text = stringResource(R.string.materials_section_disciplines),
            modifier = Modifier.fadeUpOnAppear(delayMs = 160),
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            overview.disciplines.forEachIndexed { index, discipline ->
                HubDisciplineCard(
                    discipline = discipline,
                    onTap = { onOpenDiscipline(discipline) },
                    modifier = Modifier.fadeUpOnAppear(delayMs = 200 + index * 50),
                )
            }
        }

        Text(
            text = stringResource(R.string.materials_hub_footer),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp, start = 4.dp, end = 4.dp)
                .fadeUpOnAppear(delayMs = 320),
        )
    }
}

// The "feito por estudantes" pitch card with the Contribuir CTA and the
// saved-bookmarks count.
@Composable
private fun HubHero(
    savedCount: Int,
    onContribute: () -> Unit,
    onOpenSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fixed = MaterialTheme.melon.fixed
    MaterialsHeroPlate(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp)) {
            MaterialsHeroBadge()
            Text(
                text = stringResource(R.string.materials_hero_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 26.sp,
                    letterSpacing = (-0.44).sp,
                ),
                color = fixed.onHero,
                modifier = Modifier.padding(top = 13.dp),
            )
            Text(
                text = stringResource(R.string.materials_hero_subtitle),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = fixed.onHero.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 18.dp),
            ) {
                Button(
                    onClick = onContribute,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = fixed.surfaceLight,
                        contentColor = fixed.onSurfaceLight,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = stringResource(R.string.materials_hero_contribute),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(fixed.onHero.copy(alpha = 0.16f))
                        .border(1.dp, fixed.onHero.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenSaved)
                        .padding(horizontal = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = stringResource(R.string.materials_hero_saved_hint),
                        tint = fixed.onHero,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = savedCount.toString(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = fixed.onHero,
                    )
                }
            }
        }
    }
}

@Composable
private fun HubDisciplineCard(
    discipline: MaterialsDiscipline,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = ColorFor.discipline(discipline.code)
    MaterialsCard(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(16.dp),
        ) {
            DisciplineCodeTile(code = discipline.code, tint = tint, size = 46)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = discipline.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.31).sp,
                        lineHeight = 19.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                )
                Spacer(Modifier.height(6.dp))
                if (discipline.total == 0) {
                    Text(
                        text = stringResource(R.string.materials_disc_empty_hint),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.outline,
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MaterialTypeOrder.forEach { type ->
                            val count = discipline.counts[type] ?: 0
                            if (count > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(type.hue()),
                                    )
                                    Text(
                                        text = pluralStringResource(type.tallyRes(), count, count),
                                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.align(Alignment.CenterVertically),
            ) {
                if (discipline.total == 0) {
                    Text(
                        text = stringResource(R.string.materials_disc_empty_tag),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HubError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 90.dp),
    ) {
        Text(
            text = stringResource(R.string.materials_error_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.materials_error_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.materials_error_retry))
        }
    }
}
