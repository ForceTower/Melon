package dev.forcetower.unes.ui.feature.materials.upload

import android.app.Activity
import android.text.format.Formatter
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import dev.forcetower.melon.feature.materials.domain.model.MaterialType
import dev.forcetower.melon.feature.materials.domain.model.MaterialsDiscipline
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.materials.MaterialTypeOrder
import dev.forcetower.unes.ui.feature.materials.components.DisciplineCodeTile
import dev.forcetower.unes.ui.feature.materials.components.MaterialsCard
import dev.forcetower.unes.ui.feature.materials.hue
import dev.forcetower.unes.ui.feature.materials.icon
import dev.forcetower.unes.ui.feature.materials.labelRes
import dev.forcetower.unes.ui.feature.overview.ColorFor

// The "Contribuir" wizard in an M3 bottom sheet (dc upload flow): discipline
// pick → source → details → guidelines → success. Hosted by the hub, list,
// and rejected-status screens; state lives in the shared
// `MaterialsUploadViewModel`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MaterialsUploadSheet(vm: MaterialsUploadViewModel) {
    val state by vm.state.collectAsStateWithLifecycle()
    if (!state.isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.onIntent(MaterialsUploadIntent.Dismiss) },
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(bottom = 26.dp)) {
            if (state.step != MaterialsUploadStep.Success) {
                SheetHeader(state = state, onIntent = vm::onIntent)
            }
            if (state.showsProgress) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                ) {
                    val reached = if (state.step == MaterialsUploadStep.Guidelines) 2 else 1
                    repeat(2) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (index < reached) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                ),
                        )
                    }
                }
            }

            when (state.step) {
                MaterialsUploadStep.PickDiscipline -> DisciplineStep(state, vm::onIntent)
                MaterialsUploadStep.Source -> SourceStep(state, vm::onIntent)
                MaterialsUploadStep.Details -> DetailsStep(state, vm::onIntent)
                MaterialsUploadStep.Guidelines -> GuidelinesStep(state, vm::onIntent)
                MaterialsUploadStep.Success -> SuccessStep(state, vm::onIntent)
            }
        }
    }
}

@Composable
private fun SheetHeader(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
    ) {
        if (state.showsBack) {
            HeaderChip(
                icon = Icons.Filled.ChevronLeft,
                contentDescription = stringResource(R.string.materials_back),
                onTap = { onIntent(MaterialsUploadIntent.Back) },
            )
        } else {
            Spacer(Modifier.width(32.dp))
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = stringResource(
                    when (state.step) {
                        MaterialsUploadStep.PickDiscipline -> R.string.materials_upload_title_disc
                        MaterialsUploadStep.Source -> R.string.materials_upload_title_source
                        MaterialsUploadStep.Details -> R.string.materials_upload_title_details
                        else -> R.string.materials_upload_title_guidelines
                    },
                ),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.32).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            val discipline = state.discipline
            if (discipline != null && state.step != MaterialsUploadStep.PickDiscipline) {
                Text(
                    text = "${discipline.code} · ${discipline.name}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        HeaderChip(
            icon = Icons.Filled.Close,
            contentDescription = stringResource(R.string.materials_close),
            onTap = { onIntent(MaterialsUploadIntent.Dismiss) },
        )
    }
}

@Composable
private fun HeaderChip(
    icon: ImageVector,
    contentDescription: String,
    onTap: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

// ───────── step 0 · discipline pick (hub entry only) ─────────

@Composable
private fun DisciplineStep(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.materials_upload_disc_prompt),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        state.options.forEach { discipline ->
            DisciplineOption(
                discipline = discipline,
                onTap = { onIntent(MaterialsUploadIntent.PickDiscipline(discipline.id)) },
            )
        }
    }
}

@Composable
private fun DisciplineOption(discipline: MaterialsDiscipline, onTap: () -> Unit) {
    MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(14.dp),
        ) {
            DisciplineCodeTile(code = discipline.code, tint = ColorFor.discipline(discipline.code))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = discipline.name,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${discipline.code} · " + pluralStringResource(
                        R.plurals.materials_count,
                        discipline.total,
                        discipline.total,
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

// ───────── step 1 · source ─────────

@Composable
private fun SourceStep(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) onIntent(MaterialsUploadIntent.FilePicked(uri))
    }
    // ML Kit document scanner: full-screen Play-services capture flow that
    // flattens the sheets into one PDF (mirrors iOS VisionKit digitization).
    val activity = LocalActivity.current
    val scanLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pdf = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pdf
            if (pdf != null) {
                onIntent(MaterialsUploadIntent.ScanPicked(pdf.uri, pdf.pageCount))
            }
        }
    }
    val launchScanner = launchScanner@{
        val host = activity ?: return@launchScanner
        val options = GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
        GmsDocumentScanning.getClient(options)
            .getStartScanIntent(host)
            .addOnSuccessListener { intentSender ->
                scanLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
    }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Text(
            text = stringResource(R.string.materials_upload_source_prompt),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 18.dp),
        )
        SourceOption(
            icon = Icons.Filled.Description,
            tint = MaterialTheme.melon.palette.teal,
            title = stringResource(R.string.materials_upload_source_file_title),
            subtitle = stringResource(R.string.materials_upload_source_file_sub),
            onTap = { filePicker.launch(arrayOf("application/pdf")) },
        )
        Spacer(Modifier.height(12.dp))
        SourceOption(
            icon = Icons.Filled.PhotoCamera,
            tint = MaterialTheme.melon.palette.magenta,
            title = stringResource(R.string.materials_upload_source_scan_title),
            subtitle = stringResource(R.string.materials_upload_source_scan_sub),
            onTap = launchScanner,
        )
        if (state.fileReadFailed) {
            Text(
                text = stringResource(R.string.materials_upload_file_read_failed),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.melon.status.bad,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier
                .padding(top = 18.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(17.dp),
            )
            Text(
                text = stringResource(R.string.materials_upload_source_note),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SourceOption(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String,
    onTap: () -> Unit,
) {
    MaterialsCard(cornerRadius = 20, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap)
                .padding(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(26.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.32).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
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

// ───────── step 2 · details ─────────

@Composable
private fun DetailsStep(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    val context = LocalContext.current
    Column {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            val file = state.file
            if (file != null) {
                MaterialsCard(cornerRadius = 16, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(13.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (file.isScan) Icons.Filled.PhotoCamera else Icons.Filled.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = pluralStringResource(
                                    R.plurals.materials_pages,
                                    file.pages,
                                    file.pages,
                                ) + " · " + Formatter.formatShortFileSize(context, file.byteCount.toLong()),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 1.dp),
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.melon.status.ok,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            FieldLabel(text = stringResource(R.string.materials_upload_field_type))
            Spacer(Modifier.height(8.dp))
            MaterialTypeOrder.chunked(2).forEach { rowTypes ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 9.dp),
                ) {
                    rowTypes.forEach { type ->
                        TypeOption(
                            type = type,
                            selected = state.type == type,
                            onTap = { onIntent(MaterialsUploadIntent.TypeChanged(type)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(9.dp))
            FieldLabel(text = stringResource(R.string.materials_upload_field_title))
            Spacer(Modifier.height(8.dp))
            SheetTextField(
                value = state.title,
                onValueChange = { onIntent(MaterialsUploadIntent.TitleChanged(it)) },
                placeholder = stringResource(R.string.materials_upload_title_placeholder),
            )

            Spacer(Modifier.height(18.dp))
            FieldLabel(text = stringResource(R.string.materials_upload_field_semester))
            Spacer(Modifier.height(8.dp))
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            state.semesterOptions.forEach { semester ->
                val selected = state.semester == semester
                Text(
                    text = semester,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = if (selected) {
                        MaterialTheme.colorScheme.inverseOnSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.inverseSurface
                            } else {
                                MaterialTheme.melon.surface.card
                            },
                        )
                        .border(
                            1.dp,
                            if (selected) Color.Transparent else MaterialTheme.melon.surface.line,
                            CircleShape,
                        )
                        .clickable { onIntent(MaterialsUploadIntent.SemesterChanged(semester)) }
                        .padding(horizontal = 15.dp, vertical = 8.dp),
                )
            }
        }
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                FieldLabel(text = stringResource(R.string.materials_upload_field_teacher))
                Text(
                    text = " " + stringResource(R.string.materials_upload_optional),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Spacer(Modifier.height(8.dp))
            SheetTextField(
                value = state.teacherName,
                onValueChange = { onIntent(MaterialsUploadIntent.TeacherChanged(it)) },
                placeholder = stringResource(R.string.materials_upload_teacher_placeholder),
            )
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        SheetCta(
            label = stringResource(R.string.materials_upload_continue),
            enabled = state.canContinue && !state.isSubmitting,
            loading = state.isSubmitting,
            errorText = if (state.submitFailed) {
                stringResource(R.string.materials_upload_submit_failed)
            } else {
                null
            },
            onTap = { onIntent(MaterialsUploadIntent.Continue) },
        )
    }
}

@Composable
private fun TypeOption(
    type: MaterialType,
    selected: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = type.hue()
    val shape = RoundedCornerShape(14.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        modifier = modifier
            .clip(shape)
            .background(if (selected) tint.copy(alpha = 0.16f) else MaterialTheme.melon.surface.card)
            .border(
                1.5.dp,
                if (selected) tint else MaterialTheme.melon.surface.line,
                shape,
            )
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 11.dp),
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(tint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = type.icon(),
                contentDescription = null,
                tint = MaterialTheme.melon.fixed.onHero,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = stringResource(type.labelRes()),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = if (selected) tint else MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.66.sp,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SheetTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.melon.surface.card,
            unfocusedContainerColor = MaterialTheme.melon.surface.card,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

// ───────── step 3 · guidelines ─────────

@Composable
private fun GuidelinesStep(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    val ok = MaterialTheme.melon.status.ok
    Column {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.materials_upload_guidelines_prompt),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 18.dp),
            )
            GuidelineRule(
                icon = Icons.Filled.Warning,
                tone = MaterialTheme.melon.status.bad,
                title = stringResource(R.string.materials_upload_rule_current_title),
                body = stringResource(R.string.materials_upload_rule_current_body),
            )
            Spacer(Modifier.height(12.dp))
            GuidelineRule(
                icon = Icons.Filled.Shield,
                tone = MaterialTheme.melon.status.warn,
                title = stringResource(R.string.materials_upload_rule_teacher_title),
                body = stringResource(R.string.materials_upload_rule_teacher_body),
            )
            Spacer(Modifier.height(12.dp))
            GuidelineRule(
                icon = Icons.Filled.AutoAwesome,
                tone = ok,
                title = stringResource(R.string.materials_upload_rule_quality_title),
                body = stringResource(R.string.materials_upload_rule_quality_body),
            )
            Spacer(Modifier.height(20.dp))

            val ackShape = RoundedCornerShape(16.dp)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ackShape)
                    .background(
                        if (state.isGuidelinesAccepted) {
                            ok.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    )
                    .border(
                        1.5.dp,
                        if (state.isGuidelinesAccepted) ok.copy(alpha = 0.4f) else Color.Transparent,
                        ackShape,
                    )
                    .clickable { onIntent(MaterialsUploadIntent.ToggleGuidelines) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .then(
                            if (state.isGuidelinesAccepted) {
                                Modifier.background(ok)
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
                    if (state.isGuidelinesAccepted) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.melon.fixed.onHero,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.materials_upload_ack),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.melon.surface.line)
        SheetCta(
            label = stringResource(R.string.materials_upload_publish),
            enabled = state.isGuidelinesAccepted && !state.isSubmitting,
            loading = state.isSubmitting,
            errorText = if (state.submitFailed) {
                stringResource(R.string.materials_upload_submit_failed)
            } else {
                null
            },
            onTap = { onIntent(MaterialsUploadIntent.Publish) },
        )
    }
}

@Composable
private fun GuidelineRule(
    icon: ImageVector,
    tone: Color,
    title: String,
    body: String,
) {
    MaterialsCard(cornerRadius = 18, modifier = Modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.padding(15.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(tone.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tone,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}

// ───────── step 4 · success ─────────

@Composable
private fun SuccessStep(
    state: MaterialsUploadUiState,
    onIntent: (MaterialsUploadIntent) -> Unit,
) {
    val warn = MaterialTheme.melon.status.warn
    val submitted = state.submitted
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .size(80.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(warn.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = warn,
                modifier = Modifier.size(38.dp),
            )
        }
        Text(
            text = stringResource(R.string.materials_upload_success_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.46).sp,
                lineHeight = 27.sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = stringResource(R.string.materials_upload_success_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (submitted != null) {
            MaterialsCard(
                cornerRadius = 16,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(submitted.type.hue()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = submitted.type.icon(),
                            contentDescription = null,
                            tint = MaterialTheme.melon.fixed.onHero,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = submitted.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${stringResource(submitted.type.labelRes())} · ${submitted.semester}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(warn.copy(alpha = 0.18f))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = warn,
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = stringResource(R.string.materials_status_pending),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = warn,
                        )
                    }
                }
            }
        }
        Button(
            onClick = { onIntent(MaterialsUploadIntent.Dismiss) },
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .height(52.dp),
        ) {
            Text(
                text = stringResource(R.string.materials_upload_done),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

// Pinned primary CTA under the step content, with the inline submit error.
@Composable
private fun SheetCta(
    label: String,
    enabled: Boolean,
    loading: Boolean,
    errorText: String?,
    onTap: () -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        if (errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.melon.status.bad,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = onTap,
            enabled = enabled,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(52.dp),
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}
