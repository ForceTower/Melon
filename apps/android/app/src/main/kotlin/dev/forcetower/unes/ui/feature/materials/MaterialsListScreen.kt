package dev.forcetower.unes.ui.feature.materials

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.materials.components.MaterialRow
import dev.forcetower.unes.ui.feature.materials.components.MaterialsBackButton
import dev.forcetower.unes.ui.feature.materials.components.MaterialsCard
import dev.forcetower.unes.ui.feature.materials.components.MaterialsHeroPlate
import dev.forcetower.unes.ui.feature.materials.components.MaterialsSectionLabel
import dev.forcetower.unes.ui.feature.materials.components.MineMaterialRow
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadEffect
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadIntent
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadSheet
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadViewModel
import dev.forcetower.unes.ui.feature.overview.ColorFor

// One discipline's shelf (dc `MateriaisScreen` list state): search + type
// chips over the published acervo, the student's own submissions under "Meus
// envios", and the pinned Contribuir CTA. Directly pushable — the route only
// needs a discipline id (code/name seeds are optional first-frame sugar).
@Composable
internal fun MaterialsListScreen(
    disciplineId: String,
    seedCode: String?,
    seedName: String?,
    onBack: () -> Unit,
    onOpenMaterial: (Material) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: MaterialsListViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val uploadVm: MaterialsUploadViewModel = hiltViewModel()

    LaunchedEffect(disciplineId) {
        vm.onIntent(MaterialsListIntent.Open(disciplineId, seedCode, seedName))
    }
    uploadVm.effects.collectAsEffect { effect ->
        if (effect is MaterialsUploadEffect.Finished) vm.onIntent(MaterialsListIntent.Reload)
    }

    val tint = ColorFor.discipline(state.code.ifBlank { disciplineId })
    val details = state.details
    val contribute = {
        val discipline = details?.discipline
        if (discipline != null) {
            uploadVm.onIntent(MaterialsUploadIntent.StartFromDiscipline(discipline))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Discipline-tinted wash pinned behind the header (dc `list.wash`).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        0f to tint.copy(alpha = 0.22f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(bottom = bottomInset + 110.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .height(48.dp),
            ) {
                MaterialsBackButton(onBack = onBack)
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                ) {
                    if (state.code.isNotBlank()) {
                        Text(
                            text = state.code,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = tint,
                            modifier = Modifier
                                .clip(RoundedCornerShape(7.dp))
                                .background(tint.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.materials_list_eyebrow),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        letterSpacing = (-0.56).sp,
                        lineHeight = 31.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .padding(top = 9.dp)
                        .fadeUpOnAppear(delayMs = 60),
                )
                if (details != null && !state.isEmpty) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.materials_available_count,
                            state.published.size,
                            state.published.size,
                        ),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                when {
                    details == null && state.loadFailed ->
                        ListError(onRetry = { vm.onIntent(MaterialsListIntent.Reload) })
                    details == null -> Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 80.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                    state.isEmpty -> ListEmptyHero(
                        disciplineName = state.name,
                        onContribute = contribute,
                    )
                    else -> ListContent(
                        state = state,
                        onQuery = { vm.onIntent(MaterialsListIntent.QueryChanged(it)) },
                        onFilter = { vm.onIntent(MaterialsListIntent.FilterChanged(it)) },
                        onOpenMaterial = onOpenMaterial,
                    )
                }
            }
        }

        // Pinned Contribuir pill over a surface fade (dc bottom CTA).
        if (details != null && !state.isEmpty) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.42f to MaterialTheme.colorScheme.surface,
                        ),
                    )
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomInset + 26.dp),
            ) {
                Button(
                    onClick = contribute,
                    shape = CircleShape,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.materials_contribute_cta),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }

    MaterialsUploadSheet(vm = uploadVm)
}

@Composable
private fun ListContent(
    state: MaterialsListUiState,
    onQuery: (String) -> Unit,
    onFilter: (MaterialType?) -> Unit,
    onOpenMaterial: (Material) -> Unit,
) {
    Column {
        TextField(
            value = state.query,
            onValueChange = onQuery,
            placeholder = {
                Text(
                    text = stringResource(R.string.materials_search_placeholder),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp),
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
            },
            singleLine = true,
            shape = CircleShape,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .fadeUpOnAppear(delayMs = 100),
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(top = 12.dp)
                .horizontalScroll(rememberScrollState())
                .fadeUpOnAppear(delayMs = 140),
        ) {
            TypeFilterChip(
                label = stringResource(R.string.materials_filter_all),
                count = state.published.size,
                selected = state.typeFilter == null,
                tint = MaterialTheme.colorScheme.primary,
                onTap = { onFilter(null) },
            )
            MaterialTypeOrder.forEach { type ->
                val count = state.countsByType[type] ?: 0
                if (count > 0) {
                    TypeFilterChip(
                        label = stringResource(type.pluralLabelRes()),
                        count = count,
                        selected = state.typeFilter == type,
                        tint = type.hue(),
                        onTap = { onFilter(type) },
                    )
                }
            }
        }

        if (state.mine.isNotEmpty()) {
            Spacer(Modifier.height(22.dp))
            MaterialsSectionLabel(text = stringResource(R.string.materials_section_mine))
            Spacer(Modifier.height(12.dp))
            MaterialsCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    state.mine.forEachIndexed { index, material ->
                        MineMaterialRow(
                            material = material,
                            onTap = { onOpenMaterial(material) },
                            showDivider = index < state.mine.lastIndex,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            MaterialsSectionLabel(
                text = state.typeFilter?.let { stringResource(it.pluralLabelRes()) }
                    ?: stringResource(R.string.materials_section_shelf),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pluralStringResource(
                    R.plurals.materials_item_count,
                    state.filtered.size,
                    state.filtered.size,
                ),
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Spacer(Modifier.height(12.dp))
        if (state.filtered.isEmpty()) {
            MaterialsCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(26.dp),
                ) {
                    Text(
                        text = stringResource(R.string.materials_no_results_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.materials_no_results_subtitle),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
            }
        } else {
            MaterialsCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    state.filtered.forEachIndexed { index, material ->
                        MaterialRow(
                            material = material,
                            onTap = { onOpenMaterial(material) },
                            showDivider = index < state.filtered.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterChip(
    label: String,
    count: Int,
    selected: Boolean,
    tint: Color,
    onTap: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onTap,
        shape = CircleShape,
        label = {
            Text(
                text = "$label $count",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.melon.surface.card,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = tint,
            selectedLabelColor = MaterialTheme.melon.fixed.onHero,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.melon.surface.line,
            selectedBorderColor = Color.Transparent,
        ),
    )
}

// Empty shelf → the mesh pitch card (dc list empty state).
@Composable
private fun ListEmptyHero(
    disciplineName: String,
    onContribute: () -> Unit,
) {
    val fixed = MaterialTheme.melon.fixed
    MaterialsHeroPlate(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 26.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(fixed.onHero.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = fixed.onHero,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = stringResource(R.string.materials_list_empty_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.42).sp,
                ),
                color = fixed.onHero,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                text = stringResource(R.string.materials_list_empty_body, disciplineName),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
                color = fixed.onHero.copy(alpha = 0.76f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 9.dp),
            )
            Button(
                onClick = onContribute,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = fixed.surfaceLight,
                    contentColor = fixed.onSurfaceLight,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(50.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.materials_list_empty_cta),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ListError(onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 70.dp),
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
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onRetry, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.materials_error_retry))
        }
    }
}
