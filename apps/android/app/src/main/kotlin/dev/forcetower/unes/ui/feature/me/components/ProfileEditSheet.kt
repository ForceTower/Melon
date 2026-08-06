package dev.forcetower.unes.ui.feature.me.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil3.compose.AsyncImage
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.me.MeIntent
import dev.forcetower.unes.ui.feature.me.PendingPhotoAction
import dev.forcetower.unes.ui.feature.me.ProfileEditState
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import dev.forcetower.unes.ui.feature.me.ProfileNameMaxLength
import java.io.File

// Profile-customization sheet — dc `EuScreen` "Editar perfil". Avatar with
// the camera FAB (routing through the source picker → circular crop), the
// display-name field with clear/restore affordances, the "vale só dentro do
// UNES" note anchored on the portal name, and the Cancelar/Salvar pill pair.
// The photo pipeline is local UI state (picker → crop); only the cropped
// JPEG bytes reach the ViewModel via `EditPhotoPicked`.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileEditSheet(
    state: ProfileEditState,
    identity: ProfileIdentity,
    onIntent: (MeIntent) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var sourceOpen by rememberSaveable { mutableStateOf(false) }
    var cropSource by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) cropSource = uri
    }
    // TakePicture writes full resolution into a FileProvider-backed cache
    // slot (`cache/camera/`, declared in file_provider_paths.xml).
    var cameraTarget by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { saved ->
        if (saved) cropSource = cameraTarget
    }

    ModalBottomSheet(
        onDismissRequest = { onIntent(MeIntent.CloseEditProfile) },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp)) {
            SheetHeader(onClose = { onIntent(MeIntent.CloseEditProfile) })
            Spacer(Modifier.height(18.dp))
            AvatarBlock(
                state = state,
                identity = identity,
                onPickPhoto = { sourceOpen = true },
                onRemovePhoto = { onIntent(MeIntent.EditPhotoRemoved) },
            )
            Spacer(Modifier.height(22.dp))
            NameField(
                value = state.pendingName,
                onValueChange = { onIntent(MeIntent.EditNameChanged(it)) },
                onClear = { onIntent(MeIntent.EditNameChanged("")) },
            )
            if (state.pendingName.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                RestoreOfficialButton(onClick = { onIntent(MeIntent.EditNameChanged("")) })
            }
            Spacer(Modifier.height(16.dp))
            LockNote(officialName = identity.officialName)
            if (state.failed) {
                Spacer(Modifier.height(12.dp))
                ErrorNote()
            }
            Spacer(Modifier.height(20.dp))
            FooterRow(
                saving = state.saving,
                onCancel = { onIntent(MeIntent.CloseEditProfile) },
                onSave = { onIntent(MeIntent.SaveProfile) },
            )
            Spacer(Modifier.height(12.dp))
        }
    }

    if (sourceOpen) {
        PhotoSourceSheet(
            onGallery = {
                sourceOpen = false
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            onCamera = {
                sourceOpen = false
                val slot = File(context.cacheDir, "camera/profile-capture.jpg")
                slot.parentFile?.mkdirs()
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    slot,
                )
                cameraTarget = uri
                cameraLauncher.launch(uri)
            },
            onDismiss = { sourceOpen = false },
        )
    }

    val cropUri = cropSource
    if (cropUri != null) {
        ProfilePhotoCropDialog(
            source = cropUri,
            onCancel = { cropSource = null },
            onConfirm = { bytes ->
                cropSource = null
                onIntent(MeIntent.EditPhotoPicked(bytes))
            },
        )
    }
}

@Composable
private fun SheetHeader(onClose: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(accent.copy(alpha = 0.16f).compositeOver(MaterialTheme.melon.surface.card)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Face,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_edit_sheet_title),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.me_edit_sheet_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_edit_sheet_close),
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.me_edit_sheet_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(21.dp),
            )
        }
    }
}

@Composable
private fun AvatarBlock(
    state: ProfileEditState,
    identity: ProfileIdentity,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
) {
    val brand = MaterialTheme.melon.brand
    val fixed = MaterialTheme.melon.fixed
    val accent = MaterialTheme.colorScheme.primary
    // What the preview shows: staged bytes beat the server photo; an explicit
    // Remove hides the server photo and falls back to the gradient initial.
    val pendingBitmap = remember(state.pendingPhoto) {
        state.pendingPhoto?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val serverUrl = identity.avatarUrl.takeIf { state.photoAction == PendingPhotoAction.Keep }
    val showsPhoto = pendingBitmap != null || serverUrl != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(108.dp)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = fixed.heroVeil,
                        spotColor = fixed.heroVeil,
                    )
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            0f to brand.amber,
                            0.52f to brand.coral,
                            1f to brand.magenta,
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = identity.avatarInitial,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                    ),
                    color = fixed.onHero,
                )
                when {
                    pendingBitmap != null -> Image(
                        bitmap = pendingBitmap,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                    serverUrl != null -> AsyncImage(
                        model = serverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
            }
            // Camera FAB pinned to the avatar's bottom-right, ringed with the
            // sheet surface so it reads as floating over the photo.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(3.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.me_edit_photo_change),
                        onClick = onPickPhoto,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = stringResource(R.string.me_edit_photo_change),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp),
                )
            }
        }
        if (showsPhoto) {
            Spacer(Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onRemovePhoto)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.melon.status.bad,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = stringResource(R.string.me_edit_photo_remove),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    color = MaterialTheme.melon.status.bad,
                )
            }
        }
    }
}

@Composable
private fun NameField(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(18.dp)
    Column {
        Text(
            text = stringResource(R.string.me_edit_name_label).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.66.sp,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(shape)
                .background(MaterialTheme.melon.surface.card)
                .border(
                    width = 2.dp,
                    color = if (focused) accent else MaterialTheme.melon.surface.line,
                    shape = shape,
                )
                .padding(horizontal = 14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Badge,
                contentDescription = null,
                tint = if (focused) accent else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(21.dp),
            )
            BasicTextField(
                value = value,
                onValueChange = { onValueChange(it.take(ProfileNameMaxLength)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.16).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                ),
                cursorBrush = SolidColor(accent),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = stringResource(R.string.me_edit_name_placeholder),
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused },
            )
            if (value.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = stringResource(R.string.me_edit_name_clear),
                            onClick = onClear,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.me_edit_name_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun RestoreOfficialButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(shape)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.me_edit_restore_official),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LockNote(officialName: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.melon.surface.card)
            .padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(19.dp),
        )
        val template = stringResource(R.string.me_edit_lock_note_format, officialName)
        // Bold just the portal name inside the sentence.
        val nameStart = template.lastIndexOf(officialName)
        Text(
            text = buildAnnotatedString {
                if (nameStart >= 0) {
                    append(template.substring(0, nameStart))
                    withStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) { append(officialName) }
                    append(template.substring(nameStart + officialName.length))
                } else {
                    append(template)
                }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 17.sp),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ErrorNote() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                MaterialTheme.melon.status.bad.copy(alpha = 0.12f)
                    .compositeOver(MaterialTheme.melon.surface.card),
            )
            .padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.melon.status.bad,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = stringResource(R.string.me_edit_error),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.5.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun FooterRow(saving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(shape)
                .border(1.dp, MaterialTheme.melon.surface.line, shape)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_edit_cancel),
                    enabled = !saving,
                    onClick = onCancel,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.me_edit_cancel),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Row(
            modifier = Modifier
                .weight(1.25f)
                .height(52.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = shape,
                    ambientColor = accent.copy(alpha = 0.34f),
                    spotColor = accent.copy(alpha = 0.34f),
                )
                .clip(shape)
                .background(accent)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.me_edit_save),
                    enabled = !saving,
                    onClick = onSave,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(19.dp),
                )
                Text(
                    text = stringResource(R.string.me_edit_save),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// ───────── source picker (dc z-55 sheet) ─────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoSourceSheet(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(horizontal = 22.dp)) {
            Text(
                text = stringResource(R.string.me_edit_source_title),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.36).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.melon.surface.card),
            ) {
                SourceRow(
                    icon = Icons.Filled.PhotoLibrary,
                    tint = MaterialTheme.melon.palette.teal,
                    title = stringResource(R.string.me_edit_source_gallery),
                    hint = stringResource(R.string.me_edit_source_gallery_hint),
                    onClick = onGallery,
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.melon.surface.line),
                )
                SourceRow(
                    icon = Icons.Filled.PhotoCamera,
                    tint = MaterialTheme.melon.palette.amber,
                    title = stringResource(R.string.me_edit_source_camera),
                    hint = stringResource(R.string.me_edit_source_camera_hint),
                    onClick = onCamera,
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SourceRow(
    icon: ImageVector,
    tint: androidx.compose.ui.graphics.Color,
    title: String,
    hint: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tint.copy(alpha = 0.16f).compositeOver(MaterialTheme.melon.surface.card)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
