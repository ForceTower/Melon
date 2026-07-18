package dev.forcetower.unes.ui.feature.materials

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.materials.domain.model.Material
import dev.forcetower.melon.feature.materials.domain.model.MaterialFileKind
import dev.forcetower.melon.feature.materials.domain.model.MaterialReportReason
import dev.forcetower.melon.feature.materials.domain.model.MaterialStatus
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.mvi.collectAsEffect
import dev.forcetower.unes.ui.feature.materials.components.MaterialTypeBadge
import dev.forcetower.unes.ui.feature.materials.components.MaterialsBackButton
import dev.forcetower.unes.ui.feature.materials.components.MaterialsCard
import dev.forcetower.unes.ui.feature.materials.components.MaterialsSectionLabel
import dev.forcetower.unes.ui.feature.materials.components.MaterialsToastOverlay
import dev.forcetower.unes.ui.feature.materials.components.formatDownloads
import dev.forcetower.unes.ui.feature.materials.components.icon
import dev.forcetower.unes.ui.feature.materials.components.label
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadIntent
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadSheet
import dev.forcetower.unes.ui.feature.materials.upload.MaterialsUploadViewModel
import dev.forcetower.unes.ui.feature.overview.ColorFor
import java.io.File

// Material detail (dc `MateriaisScreen` detail state) — preview mock, útil/
// salvar toggles, metadata, semi-anonymous author, report sheet, and the
// pinned "Abrir PDF" CTA. The student's own pending/rejected uploads render
// the moderation-status variant instead (dc status state).
@Composable
internal fun MaterialsDetailScreen(
    materialId: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: MaterialsDetailViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val uploadVm: MaterialsUploadViewModel = hiltViewModel()
    val context = LocalContext.current

    // The row tap seeded the material in-memory (`Seed`); this only fetches
    // after process death, a deeplink entry, or a stale shared-VM payload.
    LaunchedEffect(materialId) {
        vm.onIntent(MaterialsDetailIntent.Ensure(materialId))
    }
    vm.effects.collectAsEffect { effect ->
        when (effect) {
            is MaterialsDetailEffect.ViewFile -> viewFile(context, effect.file, effect.mimeType)
        }
    }

    val material = state.material

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        when {
            material == null && state.loadFailed -> DetailUnavailable(onBack = onBack)
            material == null -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            state.showsModerationStatus -> ModerationStatusContent(
                material = material,
                onBack = onBack,
                onResubmit = {
                    uploadVm.onIntent(
                        MaterialsUploadIntent.StartFromDiscipline(material.asDiscipline()),
                    )
                },
                bottomInset = bottomInset,
            )
            else -> DetailContent(
                material = material,
                isOpening = state.isOpening,
                onBack = onBack,
                onIntent = vm::onIntent,
                bottomInset = bottomInset,
            )
        }

        MaterialsToastOverlay(
            toast = state.toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 20.dp, end = 20.dp, bottom = bottomInset + 110.dp),
        )
    }

    if (state.isReportOpen && material != null) {
        ReportSheet(
            selected = state.reportReason,
            onPick = { vm.onIntent(MaterialsDetailIntent.PickReportReason(it)) },
            onConfirm = { vm.onIntent(MaterialsDetailIntent.ConfirmReport) },
            onDismiss = { vm.onIntent(MaterialsDetailIntent.CloseReport) },
        )
    }

    MaterialsUploadSheet(vm = uploadVm)
}

@Composable
private fun DetailContent(
    material: Material,
    isOpening: Boolean,
    onBack: () -> Unit,
    onIntent: (MaterialsDetailIntent) -> Unit,
    bottomInset: Dp,
) {
    val tint = material.type.hue()
    val ok = MaterialTheme.melon.status.ok

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    Box(modifier = Modifier.fillMaxSize()) {
        // Type-tinted wash behind the header (dc `det.wash`).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        0f to tint.copy(alpha = 0.18f),
                        1f to Color.Transparent,
                    ),
                ),
        )

        // The app bar stays pinned; the header and the cards scroll
        // beneath it.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp)
                    .height(48.dp),
            ) {
                MaterialsBackButton(onBack = onBack)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onIntent(MaterialsDetailIntent.OpenReport) }) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = stringResource(R.string.materials_report),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            PinnedHeaderHairline(scrolled = scrolled)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(bottom = bottomInset + 110.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .fadeUpOnAppear(delayMs = 20),
                ) {
                    PreviewCard(material = material)
                    Column(modifier = Modifier.weight(1f).padding(top = 4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(tint.copy(alpha = 0.20f))
                                .padding(horizontal = 9.dp, vertical = 3.dp),
                        ) {
                            Icon(
                                imageVector = material.type.icon(),
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(12.dp),
                            )
                            Text(
                                text = stringResource(material.type.labelRes()).uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = tint,
                            )
                        }
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.44).sp,
                                lineHeight = 26.sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 12.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ThumbUp,
                                    contentDescription = stringResource(R.string.materials_action_useful),
                                    tint = if (material.isUseful) ok else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(15.dp),
                                )
                                Text(
                                    text = material.usefulCount.toString(),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                    color = if (material.isUseful) ok else MaterialTheme.colorScheme.outline,
                                )
                            }
                            Text(
                                text = "·",
                                color = MaterialTheme.colorScheme.outline,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = formatDownloads(material.downloadCount),
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 18.dp)
                        .fadeUpOnAppear(delayMs = 80),
                ) {
                    ToggleActionButton(
                        label = stringResource(R.string.materials_action_useful),
                        icon = Icons.Filled.ThumbUp,
                        active = material.isUseful,
                        onTap = { onIntent(MaterialsDetailIntent.ToggleUseful) },
                        modifier = Modifier.weight(1f),
                    )
                    ToggleActionButton(
                        label = stringResource(
                            if (material.isSaved) R.string.materials_action_saved
                            else R.string.materials_action_save,
                        ),
                        icon = Icons.Filled.Bookmark,
                        active = material.isSaved,
                        onTap = { onIntent(MaterialsDetailIntent.ToggleSave) },
                        modifier = Modifier.weight(1f),
                    )
                }

                val note = material.note
                if (note != null) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .padding(top = 22.dp)
                            .fadeUpOnAppear(delayMs = 140),
                    ) {
                        MaterialsSectionLabel(text = stringResource(R.string.materials_detail_about))
                        Spacer(Modifier.height(12.dp))
                        MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
                            Row(modifier = Modifier.padding(horizontal = 17.dp, vertical = 15.dp)) {
                                Box(
                                    modifier = Modifier
                                        .width(4.dp)
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(tint),
                                )
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.5.sp,
                                        lineHeight = 22.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(start = 12.dp),
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 22.dp)
                        .fadeUpOnAppear(delayMs = 200),
                ) {
                    MaterialsSectionLabel(text = stringResource(R.string.materials_detail_details))
                    Spacer(Modifier.height(12.dp))
                    MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            DetailMetaRow(
                                icon = Icons.Filled.GridView,
                                label = stringResource(R.string.materials_detail_row_discipline),
                                value = material.discipline.code,
                                showDivider = true,
                            )
                            DetailMetaRow(
                                icon = Icons.Filled.Schedule,
                                label = stringResource(R.string.materials_detail_row_semester),
                                value = material.semester,
                                showDivider = true,
                            )
                            DetailMetaRow(
                                icon = Icons.Filled.Person,
                                label = stringResource(R.string.materials_detail_row_teacher),
                                value = material.teacherName
                                    ?: stringResource(R.string.materials_detail_teacher_unknown),
                                showDivider = true,
                            )
                            DetailMetaRow(
                                icon = material.fileKind.icon(),
                                label = stringResource(R.string.materials_detail_row_file),
                                value = "${material.fileKind.label()} · " +
                                    pluralStringResource(
                                        R.plurals.materials_pages,
                                        material.pages,
                                        material.pages,
                                    ),
                                showDivider = false,
                            )
                        }
                    }
                }

                UploaderCard(
                    material = material,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .padding(top = 22.dp)
                        .fadeUpOnAppear(delayMs = 260),
                )

                Text(
                    text = stringResource(R.string.materials_detail_disclaimer),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, lineHeight = 17.sp),
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, start = 30.dp, end = 30.dp),
                )
            }
        }

        // Pinned "Abrir PDF/foto" CTA (dc bottom CTA).
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
                onClick = { onIntent(MaterialsDetailIntent.OpenFile) },
                enabled = !isOpening,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
            ) {
                if (isOpening) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(9.dp))
                    Text(
                        text = stringResource(
                            when (material.fileKind) {
                                MaterialFileKind.Pdf -> R.string.materials_open_pdf
                                MaterialFileKind.Photo -> R.string.materials_open_photo
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            }
        }
    }
}

// Decorative 3:4 page mock (dc preview card): striped paper, skeleton text
// lines, type badge, and the file-kind strip.
@Composable
private fun PreviewCard(material: Material) {
    val tint = material.type.hue()
    val ink = MaterialTheme.colorScheme.onBackground
    MaterialsCard(cornerRadius = 14) {
        Box(
            modifier = Modifier
                .width(132.dp)
                .aspectRatio(3f / 4f),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .height(7.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(tint.copy(alpha = 0.85f)),
                )
                listOf(0.88f, 0.96f, 0.72f, 0.90f, 0.60f, 0.84f, 0.94f, 0.68f).forEach { widthFraction ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(widthFraction)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(ink.copy(alpha = 0.14f)),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(24.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(tint),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = material.type.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.melon.fixed.onHero,
                    modifier = Modifier.size(14.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.melon.fixed.heroVeil.copy(alpha = 0.5f))
                    .padding(horizontal = 9.dp, vertical = 5.dp),
            ) {
                Icon(
                    imageVector = material.fileKind.icon(),
                    contentDescription = null,
                    tint = MaterialTheme.melon.fixed.onHero,
                    modifier = Modifier.size(13.dp),
                )
                Text(
                    text = "${material.fileKind.label().uppercase()} · ${material.pages}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.melon.fixed.onHero,
                )
            }
        }
    }
}

// "Útil" / "Salvar" pill — neutral plate that flips to the green success
// treatment when active.
@Composable
private fun ToggleActionButton(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ok = MaterialTheme.melon.status.ok
    val shape = RoundedCornerShape(14.dp)
    val background = if (active) ok.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh
    val border = if (active) ok.copy(alpha = 0.45f) else Color.Transparent
    val content = if (active) ok else MaterialTheme.colorScheme.onSurface
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, border, shape)
            .clickable(onClick = onTap),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(17.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = content,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

@Composable
private fun DetailMetaRow(
    icon: ImageVector,
    label: String,
    value: String,
    showDivider: Boolean,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 11.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        if (showDivider) {
            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        }
    }
}

// Semi-anonymous author: course + entry year, never a name.
@Composable
private fun UploaderCard(material: Material, modifier: Modifier = Modifier) {
    val tint = ColorFor.discipline(material.discipline.code)
    MaterialsCard(cornerRadius = 18, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(tint, MaterialTheme.melon.brand.plum),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.melon.fixed.onHero,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = stringResource(R.string.materials_uploader_name, material.uploader.course),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = stringResource(R.string.materials_uploader_sub, material.uploader.entryYear),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

// ───────── moderation status (own pending/rejected uploads) ─────────

@Composable
private fun ModerationStatusContent(
    material: Material,
    onBack: () -> Unit,
    onResubmit: () -> Unit,
    bottomInset: Dp,
) {
    val pending = material.status == MaterialStatus.Pending
    val tone = if (pending) MaterialTheme.melon.status.warn else MaterialTheme.melon.status.bad

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // The app bar stays pinned; the status content scrolls beneath it.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp)
                .height(48.dp),
        ) {
            MaterialsBackButton(onBack = onBack)
            Text(
                text = stringResource(
                    if (pending) R.string.materials_status_pending
                    else R.string.materials_status_rejected,
                ),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(48.dp))
        }
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 40.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
                    .fadeUpOnAppear(delayMs = 20),
            ) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(tone.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (pending) Icons.Filled.Schedule else Icons.Filled.Warning,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(38.dp),
                    )
                }
                Text(
                    text = stringResource(
                        if (pending) R.string.materials_moderation_pending_title
                        else R.string.materials_moderation_rejected_title,
                    ),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.5).sp,
                        lineHeight = 29.sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 20.dp),
                )
                Text(
                    text = stringResource(
                        if (pending) R.string.materials_moderation_pending_body
                        else R.string.materials_moderation_rejected_body,
                    ),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp, start = 18.dp, end = 18.dp),
                )
            }

            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 24.dp)
                    .fadeUpOnAppear(delayMs = 100),
            ) {
                MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.padding(16.dp),
                    ) {
                        MaterialTypeBadge(material)
                        Column {
                            Text(
                                text = material.title,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = (-0.3).sp,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Text(
                                text = "${stringResource(material.type.labelRes())} · ${material.semester} · " +
                                    pluralStringResource(
                                        R.plurals.materials_pages_short,
                                        material.pages,
                                        material.pages,
                                    ),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (pending) {
                    ModerationTimeline()
                } else {
                    val reason = material.rejectionReason
                    if (reason != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(tone.copy(alpha = 0.12f))
                                .border(1.dp, tone.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(tone),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.melon.fixed.onHero,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.materials_moderation_reason_title),
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                )
                                Text(
                                    text = reason,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 13.sp,
                                        lineHeight = 19.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(top = 3.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                    Button(
                        onClick = onResubmit,
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Text(
                            text = stringResource(R.string.materials_moderation_resubmit),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModerationTimeline() {
    val ok = MaterialTheme.melon.status.ok
    val warn = MaterialTheme.melon.status.warn
    val steps = listOf(
        Triple(R.string.materials_moderation_step_sent, R.string.materials_moderation_step_sent_sub, ok),
        Triple(R.string.materials_moderation_step_review, R.string.materials_moderation_step_review_sub, warn),
        Triple(
            R.string.materials_moderation_step_published,
            R.string.materials_moderation_step_published_sub,
            null,
        ),
    )
    MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            steps.forEachIndexed { index, (labelRes, subRes, tone) ->
                Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(tone ?: MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (index == 0) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.melon.fixed.onHero,
                                    modifier = Modifier.size(12.dp),
                                )
                            }
                        }
                        if (index < steps.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .width(2.dp)
                                    .height(26.dp)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            )
                        }
                    }
                    Column(modifier = Modifier.padding(bottom = 14.dp)) {
                        Text(
                            text = stringResource(labelRes),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = if (tone != null) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                        Text(
                            text = stringResource(subRes),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 1.dp),
                        )
                    }
                }
            }
        }
    }
}

// ───────── report sheet ─────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportSheet(
    selected: MaterialReportReason?,
    onPick: (MaterialReportReason) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.materials_report),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.4).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.materials_report_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.5.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            )
            MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
                Column {
                    ReportReasons.forEachIndexed { index, (reason, labelRes) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(reason) }
                                .padding(horizontal = 15.dp, vertical = 13.dp),
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                            )
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (selected == reason) {
                                            Modifier.background(MaterialTheme.colorScheme.primary)
                                        } else {
                                            Modifier.border(
                                                1.5.dp,
                                                MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape,
                                            )
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (selected == reason) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                        }
                        if (index < ReportReasons.lastIndex) {
                            HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
                        }
                    }
                }
            }
            Button(
                onClick = onConfirm,
                enabled = selected != null,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp)
                    .height(52.dp),
            ) {
                Text(
                    text = stringResource(R.string.materials_report_confirm),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

private val ReportReasons = listOf(
    MaterialReportReason.Illegible to R.string.materials_report_reason_illegible,
    MaterialReportReason.OngoingExam to R.string.materials_report_reason_ongoing,
    MaterialReportReason.RestrictedByTeacher to R.string.materials_report_reason_restricted,
    MaterialReportReason.WrongDiscipline to R.string.materials_report_reason_wrong,
    MaterialReportReason.Other to R.string.materials_report_reason_other,
)

@Composable
private fun DetailUnavailable(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MaterialsBackButton(onBack = onBack)
        }
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
            TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                Text(text = stringResource(R.string.materials_back))
            }
        }
    }
}

// A rejected upload re-opens the wizard locked to its own discipline; the
// counts don't matter for the flow, so an empty map suffices.
private fun Material.asDiscipline() = MaterialsDiscipline(
    id = discipline.id,
    code = discipline.code,
    name = discipline.name,
    teacherName = teacherName,
    counts = emptyMap(),
)

// Hands the downloaded copy to the system viewer through the app's
// FileProvider grant — same mechanism as the Me documents sheet.
private fun viewFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No viewer installed — nothing sensible to do.
    }
}
