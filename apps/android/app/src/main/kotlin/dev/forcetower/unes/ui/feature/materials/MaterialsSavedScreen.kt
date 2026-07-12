package dev.forcetower.unes.ui.feature.materials

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.ui.feature.materials.components.MaterialRow
import dev.forcetower.unes.ui.feature.materials.components.MaterialsBackButton
import dev.forcetower.unes.ui.feature.materials.components.MaterialsCard
import dev.forcetower.unes.ui.feature.materials.components.MaterialsSectionLabel

// "Salvos" — the server-side bookmark shelf grouped by discipline, reached
// from the hub hero's bookmark counter. Mirrors iOS `MaterialsSavedView`.
@Composable
internal fun MaterialsSavedScreen(
    onBack: () -> Unit,
    onOpenMaterial: (Material) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: MaterialsSavedViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.onIntent(MaterialsSavedIntent.Load) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(bottom = bottomInset + 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(48.dp),
        ) {
            MaterialsBackButton(onBack = onBack)
        }
        Text(
            text = stringResource(R.string.materials_saved_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fadeUpOnAppear(delayMs = 20),
        )

        val materials = state.materials
        when {
            materials == null && state.loadFailed -> SavedMessage(
                title = stringResource(R.string.materials_error_title),
                body = stringResource(R.string.materials_error_subtitle),
            ) {
                TextButton(onClick = { vm.onIntent(MaterialsSavedIntent.Load) }) {
                    Text(text = stringResource(R.string.materials_error_retry))
                }
            }
            materials == null -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 100.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            materials.isEmpty() -> SavedMessage(
                title = stringResource(R.string.materials_saved_empty_title),
                body = stringResource(R.string.materials_saved_empty_body),
            )
            else -> Column(
                verticalArrangement = Arrangement.spacedBy(22.dp),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
                    .fadeUpOnAppear(delayMs = 80),
            ) {
                state.groups.forEach { (disciplineName, items) ->
                    Column {
                        MaterialsSectionLabel(text = disciplineName)
                        Spacer(Modifier.height(12.dp))
                        MaterialsCard(modifier = Modifier.fillMaxWidth()) {
                            Column {
                                items.forEachIndexed { index, material ->
                                    MaterialRow(
                                        material = material,
                                        onTap = { onOpenMaterial(material) },
                                        showDivider = index < items.lastIndex,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedMessage(
    title: String,
    body: String,
    action: @Composable () -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 90.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        action()
    }
}
