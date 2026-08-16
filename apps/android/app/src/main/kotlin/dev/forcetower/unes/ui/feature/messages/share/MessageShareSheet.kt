package dev.forcetower.unes.ui.feature.messages.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.Message
import dev.forcetower.unes.ui.feature.messages.MessagesFixtures
import dev.forcetower.unes.ui.feature.messages.fullTime
import dev.forcetower.unes.ui.feature.messages.paragraphs
import dev.forcetower.unes.ui.feature.messages.rememberMessageRoleStrings
import dev.forcetower.unes.ui.feature.messages.toUi
import kotlinx.coroutines.launch

// What the sheet did, so the detail screen behind it can raise the matching
// snackbar once the sheet is gone. Sharing isn't here on purpose: the system
// chooser is its own confirmation, and a snackbar queued behind it would land
// after the fact even when the chooser was dismissed.
internal enum class MessageShareOutcome {
    ImageSaved,
    ImageSaveFailed,
    TextCopied,
}

private enum class ShareMode(val labelRes: Int, val subtitleRes: Int) {
    Image(R.string.messages_share_mode_image, R.string.messages_share_subtitle_image),
    Text(R.string.messages_share_mode_text, R.string.messages_share_subtitle_text),
}

// M3 modal bottom sheet from the dc `MessagesScreen` share flow: segmented
// button to swap formats without leaving the sheet, a preview of exactly what
// will be sent, then the filled share action and the outlined save/copy one.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageShareSheet(
    message: Message,
    onDismiss: () -> Unit,
    onOutcome: (MessageShareOutcome) -> Unit,
) {
    val context = LocalContext.current
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(ShareMode.Image) }

    val plainText = messageShareText(message)
    val chooserTitle = stringResource(R.string.messages_share_chooser_title)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Header(mode = mode, onClose = onDismiss)

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                ShareMode.entries.forEachIndexed { index, entry ->
                    SegmentedButton(
                        selected = mode == entry,
                        onClick = { mode = entry },
                        shape = SegmentedButtonDefaults.itemShape(index, ShareMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.22f)
                                .compositeOver(MaterialTheme.colorScheme.surface),
                            activeContentColor = MaterialTheme.colorScheme.primary,
                            activeBorderColor = MaterialTheme.melon.surface.cardLine,
                            inactiveBorderColor = MaterialTheme.melon.surface.cardLine,
                        ),
                        label = {
                            Text(
                                text = stringResource(entry.labelRes),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontSize = 14.sp,
                                    fontWeight = if (mode == entry) FontWeight.ExtraBold else FontWeight.SemiBold,
                                ),
                            )
                        },
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                when (mode) {
                    ShareMode.Image -> ImagePreview(message = message, layer = layer)
                    ShareMode.Text -> TextPreview(message = message)
                }
            }

            HorizontalDivider(color = MaterialTheme.melon.surface.line)

            Actions(
                mode = mode,
                onShare = {
                    when (mode) {
                        ShareMode.Image -> scope.launch {
                            layer.toShareBitmap()?.let { shareImage(context, it, chooserTitle) }
                            onDismiss()
                        }
                        ShareMode.Text -> {
                            shareText(context, plainText, chooserTitle)
                            onDismiss()
                        }
                    }
                },
                onSecondary = {
                    when (mode) {
                        ShareMode.Image -> scope.launch {
                            val bitmap = layer.toShareBitmap()
                            val saved = bitmap != null && saveImageToDownloads(context, bitmap)
                            onOutcome(
                                if (saved) MessageShareOutcome.ImageSaved
                                else MessageShareOutcome.ImageSaveFailed,
                            )
                            onDismiss()
                        }
                        ShareMode.Text -> {
                            copyText(context, plainText)
                            if (!platformConfirmsCopy) onOutcome(MessageShareOutcome.TextCopied)
                            onDismiss()
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun Header(mode: ShareMode, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, bottom = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.messages_share_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 22.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(mode.subtitleRes),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.messages_share_close_a11y),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ImagePreview(message: Message, layer: GraphicsLayer) {
    // Laid out at the width whose pixel size is the dc's 1080 grid, so what the
    // layer captures is already export resolution. Narrow screens fall back to
    // whatever fits and the export resamples.
    val exact = exportCardWidth(LocalDensity.current.density).dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        MessageShareCard(
            message = message,
            width = minOf(exact, maxWidth),
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .recordShareCard(layer),
        )
    }
}

@Composable
private fun TextPreview(message: Message) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.melon.surface.card)
                .border(1.dp, MaterialTheme.melon.surface.line, shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message.sender.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = message.sender.role,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
            Text(
                text = fullTime(message.receivedAt),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            message.paragraphs().forEach { paragraph ->
                Text(
                    text = paragraph,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(R.string.messages_share_card_footer),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
        }
    }
}

@Composable
private fun Actions(mode: ShareMode, onShare: () -> Unit, onSecondary: () -> Unit) {
    val showSecondary = mode == ShareMode.Text || canSaveToDownloads
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onShare,
            shape = CircleShape,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
        ) {
            ActionLabel(Icons.Filled.Share, R.string.messages_share_action)
        }

        if (showSecondary) {
            OutlinedButton(
                onClick = onSecondary,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                if (mode == ShareMode.Image) {
                    ActionLabel(Icons.Filled.Download, R.string.messages_share_save_image)
                } else {
                    ActionLabel(Icons.Filled.ContentCopy, R.string.messages_share_copy_text)
                }
            }
        }
    }
}

@Composable
private fun ActionLabel(icon: ImageVector, labelRes: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// The plain-text form: same content as the card, nothing that needs the app to
// read it.
@Composable
private fun messageShareText(message: Message): String {
    val footer = stringResource(R.string.messages_share_card_footer)
    val lines = buildList {
        add(message.sender.name)
        add(message.sender.role)
        add(fullTime(message.receivedAt))
        add("")
        addAll(message.paragraphs())
        add("")
        add(footer)
    }
    return lines.joinToString(separator = "\n")
}

@Preview
@Composable
private fun MessageShareSheetPreview() {
    MelonTheme {
        val roles = rememberMessageRoleStrings()
        val seed = MessagesFixtures.items[1]
        val message = MessagesFixtures.detailById[seed.id]?.toUi(roles) ?: seed.toUi(roles)
        MessageShareSheet(message = message, onDismiss = {}, onOutcome = {})
    }
}
