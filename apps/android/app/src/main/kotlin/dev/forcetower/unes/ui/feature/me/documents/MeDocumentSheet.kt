package dev.forcetower.unes.ui.feature.me.documents

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.forcetower.melon.feature.me.domain.model.AcademicDocument
import dev.forcetower.melon.feature.me.domain.model.DocumentFetchError
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.disciplines.formatGrade
import dev.forcetower.unes.ui.feature.me.DocumentSheetState
import dev.forcetower.unes.ui.feature.me.DocumentStage
import dev.forcetower.unes.ui.feature.me.ProfileIdentity
import dev.forcetower.unes.ui.feature.me.ShortcutTone
import dev.forcetower.unes.ui.feature.me.hue
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// The Comprovante / Histórico M3 bottom sheet — dc `EuScreen` document sheet.
// Summary rows on top; below them the stage area walks the same machine as
// iOS `MeDocumentFeature`: download CTA → (captcha) → spinner → offline copy
// card with fresh/stale badges, or the failure banner with retry.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MeDocumentSheet(
    sheet: DocumentSheetState,
    identity: ProfileIdentity?,
    captchaSiteKey: String,
    captchaBaseUrl: String,
    onRequest: () -> Unit,
    onCaptchaSolved: (String) -> Unit,
    onCaptchaCanceled: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hue = sheet.document.tone().hue()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // Content-hugging sheets must open fully — the half-expanded stop
        // clips the secondary button (same setting the About sheet uses).
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 26.dp)) {
            Header(document = sheet.document, hue = hue, onClose = onDismiss)
            Spacer(Modifier.height(20.dp))
            SummaryRows(document = sheet.document, identity = identity, hue = hue)

            when (val stage = sheet.stage) {
                DocumentStage.Intro -> Intro(hue = hue, onDownload = onRequest)
                DocumentStage.Captcha -> Captcha(
                    siteKey = captchaSiteKey,
                    baseUrl = captchaBaseUrl,
                    showCancel = sheet.stored != null,
                    onToken = onCaptchaSolved,
                    onCancel = onCaptchaCanceled,
                )
                DocumentStage.Generating -> Generating(sheet = sheet, hue = hue)
                DocumentStage.Saved, DocumentStage.Fresh, is DocumentStage.Stale -> {
                    val stored = sheet.stored
                    if (stored != null) {
                        DocumentReady(
                            stored = stored,
                            stage = stage,
                            hue = hue,
                            onOpen = { openPdf(context, stored.file) },
                            onRefresh = onRequest,
                        )
                    }
                }
                is DocumentStage.Failed -> Failed(reason = stage.reason, onRetry = onRequest)
            }
        }
    }
}

@Composable
private fun Header(document: AcademicDocument, hue: Color, onClose: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(hue.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = document.icon(),
                contentDescription = null,
                tint = hue,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(top = 1.dp)) {
            Text(
                text = stringResource(document.titleRes()),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 23.sp,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = stringResource(document.subtitleRes()),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        CloseButton(onClose = onClose)
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = stringResource(R.string.me_document_close),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(21.dp),
        )
    }
}

@Composable
private fun SummaryRows(document: AcademicDocument, identity: ProfileIdentity?, hue: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        SummaryRow(label = stringResource(R.string.me_document_row_student), value = identity?.name, hue = hue)
        RowDivider()
        SummaryRow(label = stringResource(R.string.me_document_row_course), value = identity?.course, hue = hue)
        RowDivider()
        when (document) {
            AcademicDocument.EnrollmentCertificate -> SummaryRow(
                label = stringResource(R.string.me_document_row_status),
                value = stringResource(R.string.me_document_row_status_active),
                hue = hue,
            )
            AcademicDocument.AcademicHistory -> SummaryRow(
                label = stringResource(R.string.me_stat_score),
                value = formatGrade(identity?.cr),
                hue = hue,
            )
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String?, hue: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            color = hue,
            modifier = Modifier.width(76.dp),
        )
        Text(
            text = value?.takeIf { it.isNotBlank() } ?: "—",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.melon.surface.line),
    )
}

// ───────── Stages ─────────

@Composable
private fun Intro(hue: Color, onDownload: () -> Unit) {
    Column {
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            label = stringResource(R.string.me_document_download),
            icon = Icons.Filled.Download,
            background = hue,
            onClick = onDownload,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = stringResource(R.string.me_document_offline_footnote),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Captcha(
    siteKey: String,
    baseUrl: String,
    showCancel: Boolean,
    onToken: (String) -> Unit,
    onCancel: () -> Unit,
) {
    Column {
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.me_document_captcha_prompt),
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(10.dp))
        RecaptchaWebView(
            siteKey = siteKey,
            baseUrl = baseUrl,
            onToken = onToken,
            modifier = Modifier
                .fillMaxWidth()
                .height(480.dp)
                .clip(RoundedCornerShape(15.dp)),
        )
        if (showCancel) {
            Spacer(Modifier.height(10.dp))
            GhostButton(
                label = stringResource(R.string.me_document_cancel),
                icon = null,
                onClick = onCancel,
            )
        }
    }
}

@Composable
private fun Generating(sheet: DocumentSheetState, hue: Color) {
    val refreshing = sheet.stored != null
    val title = when {
        refreshing -> stringResource(R.string.me_document_refreshing)
        sheet.document == AcademicDocument.EnrollmentCertificate ->
            stringResource(R.string.me_document_generating_certificate)
        else -> stringResource(R.string.me_document_generating)
    }
    val subtitle = if (refreshing) {
        stringResource(R.string.me_document_refreshing_sub)
    } else {
        stringResource(R.string.me_document_generating_sub)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CircularProgressIndicator(
            color = hue,
            trackColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
            strokeWidth = 4.dp,
            modifier = Modifier.size(46.dp),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun DocumentReady(
    stored: StoredAcademicDocument,
    stage: DocumentStage,
    hue: Color,
    onOpen: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column {
        Spacer(Modifier.height(14.dp))
        FileCard(stored = stored, stage = stage, hue = hue)
        if (stage is DocumentStage.Stale) {
            Spacer(Modifier.height(12.dp))
            StaleBanner()
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            label = stringResource(R.string.me_document_open),
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            background = hue,
            onClick = onOpen,
        )
        Spacer(Modifier.height(10.dp))
        GhostButton(
            label = stringResource(R.string.me_document_refresh),
            icon = Icons.Filled.Sync,
            onClick = onRefresh,
        )
    }
}

@Composable
private fun FileCard(stored: StoredAcademicDocument, stage: DocumentStage, hue: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PdfChip(hue = hue)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = stored.file.name,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(
                    text = stringResource(R.string.me_document_version_format, stored.version),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 12.sp,
                        letterSpacing = 0.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
            Spacer(Modifier.height(5.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.melon.status.ok),
                )
                Text(
                    text = statusLine(stage, stored),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun statusLine(stage: DocumentStage, stored: StoredAcademicDocument): String = when (stage) {
    DocumentStage.Fresh -> stringResource(R.string.me_document_status_fresh)
    is DocumentStage.Stale -> stringResource(R.string.me_document_status_stale)
    else -> stringResource(R.string.me_document_status_saved_format, formatSavedAt(stored.savedAtMs))
}

@Composable
private fun PdfChip(hue: Color) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, hue.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.me_document_pdf_badge),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.66.sp,
            ),
            color = hue,
        )
    }
}

@Composable
private fun StaleBanner() {
    val warn = MaterialTheme.melon.status.warn
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(warn.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.dp, warn.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CloudOff,
            contentDescription = null,
            tint = warn,
            modifier = Modifier.size(20.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.me_document_stale_title),
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.me_document_stale_body),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun Failed(reason: DocumentFetchError, onRetry: () -> Unit) {
    val bad = MaterialTheme.melon.status.bad
    Column {
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(bad.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
                .border(1.dp, bad.copy(alpha = 0.30f), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Error,
                contentDescription = null,
                tint = bad,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(
                        when (reason) {
                            DocumentFetchError.Unavailable -> R.string.me_document_unavailable_title
                            DocumentFetchError.Connection -> R.string.me_document_error_title
                        },
                    ),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = stringResource(
                        when (reason) {
                            DocumentFetchError.Unavailable -> R.string.me_document_unavailable_body
                            DocumentFetchError.Connection -> R.string.me_document_error_body
                        },
                    ),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            label = stringResource(R.string.me_document_retry),
            icon = Icons.Filled.Refresh,
            background = MaterialTheme.colorScheme.primary,
            onClick = onRetry,
        )
    }
}

// ───────── Buttons ─────────

@Composable
private fun PrimaryButton(
    label: String,
    icon: ImageVector,
    background: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(9.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.melon.fixed.onHero,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            color = MaterialTheme.melon.fixed.onHero,
        )
    }
}

@Composable
private fun GhostButton(
    label: String,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ───────── Helpers ─────────

internal fun AcademicDocument.tone(): ShortcutTone = when (this) {
    AcademicDocument.EnrollmentCertificate -> ShortcutTone.Indigo
    AcademicDocument.AcademicHistory -> ShortcutTone.Violet
}

private fun AcademicDocument.icon(): ImageVector = when (this) {
    AcademicDocument.EnrollmentCertificate -> Icons.Filled.Description
    AcademicDocument.AcademicHistory -> Icons.AutoMirrored.Filled.ReceiptLong
}

private fun AcademicDocument.titleRes(): Int = when (this) {
    AcademicDocument.EnrollmentCertificate -> R.string.me_document_certificate_title
    AcademicDocument.AcademicHistory -> R.string.me_document_history_title
}

private fun AcademicDocument.subtitleRes(): Int = when (this) {
    AcademicDocument.EnrollmentCertificate -> R.string.me_document_certificate_subtitle
    AcademicDocument.AcademicHistory -> R.string.me_document_history_subtitle
}

// "10 jul 2026" — month abbreviation dot stripped, matching the dc file card.
private fun formatSavedAt(savedAtMs: Long): String {
    if (savedAtMs <= 0L) return "—"
    val date = Instant.ofEpochMilli(savedAtMs).atZone(ZoneId.systemDefault()).toLocalDate()
    return DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(date).replace(".", "")
}

// Hands the offline copy to the system PDF viewer through the app's
// FileProvider grant.
private fun openPdf(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No PDF viewer installed — nothing sensible to do.
    }
}
