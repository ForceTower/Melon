package dev.forcetower.unes.ui.feature.messages

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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.components.AttachmentTile

// Route adapter sitting between the Messages tab's `MessageDetail` NavKey
// and the pure detail screen below. Owns the open/close lifecycle of the
// detail subscription on `MessagesViewModel`: opens on enter (using the
// seed already in the inbox flow), closes on dispose. Falls back through
// `openDetail` → `openSeed` → `rawItems` so the screen renders something
// even when the route is restored after process death and the inbox flow
// hasn't landed yet.
@Composable
internal fun MessageDetailRoute(
    id: String,
    vm: MessagesViewModel,
    onBack: () -> Unit,
    bottomInset: Dp = 0.dp,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val roles = rememberMessageRoleStrings()

    // Re-open on (re-)compose if the VM isn't already tracking this id.
    // Re-fires when `rawItems` lands so the seed is available after
    // process-death restoration; the `openMessageId == id` guard makes
    // subsequent inbox emits no-ops.
    LaunchedEffect(id, state.rawItems) {
        if (state.openMessageId != id) {
            val seed = state.rawItems.firstOrNull { it.id == id }
            if (seed != null) {
                vm.onIntent(MessagesIntent.OpenMessage(id, seed))
            }
        }
    }

    DisposableEffect(id) {
        onDispose {
            // Tear down the detail subscription when the entry leaves
            // composition (popped from the stack OR tab switched away).
            // Guarded so we don't clobber a different detail that's now
            // active.
            if (vm.state.value.openMessageId == id) {
                vm.onIntent(MessagesIntent.CloseMessage)
            }
        }
    }

    val rendered = state.openDetail?.takeIf { it.id == id }?.toUi(roles)
        ?: state.openSeed?.takeIf { it.id == id }?.toUi(roles)
        ?: state.rawItems.firstOrNull { it.id == id }?.toUi(roles)

    if (rendered != null) {
        MessageDetailScreen(
            message = rendered,
            onBack = onBack,
            onToggleStar = { vm.onIntent(MessagesIntent.ToggleStar(id)) },
            onAppear = { vm.onIntent(MessagesIntent.MarkRead(id)) },
            bottomInset = bottomInset,
        )
    } else {
        // Deeplink race: the push usually lands before the device has synced
        // the message it points at. `ConnectedViewModel.onAppeared` is already
        // pulling; once the inbox flow emits the id, the branch above takes
        // over. Until then, back chrome + a spinner instead of a blank frame.
        MessageDetailLoading(onBack = onBack)
    }
}

@Composable
private fun MessageDetailLoading(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.messages_back_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

// Full message detail — 2026 redesign (dc `UNES Mensagens - Android`): small
// top app bar with back + star (save) toggle, the category-tinted sender
// header card, the timestamp row, the body paragraphs with linkified URLs,
// the attachment tiles, and the "salva" note pill when starred.
@Composable
internal fun MessageDetailScreen(
    message: Message,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onToggleStar: () -> Unit = {},
    bottomInset: Dp = 0.dp,
    onAppear: (() -> Unit)? = null,
) {
    val hue = categoryColor(message.category)
    val images = message.attachments.filter { it.kind == MessageAttachmentKind.Image }
    val nonImages = message.attachments.filter { it.kind != MessageAttachmentKind.Image }

    LaunchedEffect(message.id) {
        onAppear?.invoke()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .verticalScroll(rememberScrollState())
            .padding(bottom = bottomInset),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 8.dp)
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.messages_back_label),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StarToggle(starred = message.starred, onToggle = onToggleStar)
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(top = 12.dp, bottom = 32.dp)) {
            SenderHeaderCard(
                message = message,
                hue = hue,
                modifier = Modifier.fadeUpOnAppear(delayMs = 60),
            )

            Row(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(top = 20.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(17.dp),
                )
                Text(
                    text = fullTime(message.receivedAt),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp, bottom = 18.dp)
                    .height(1.dp)
                    .background(MaterialTheme.melon.surface.line),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fadeUpOnAppear(delayMs = 140),
            ) {
                if (!message.subject.isNullOrBlank()) {
                    Text(
                        text = message.subject,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 21.sp,
                            lineHeight = 26.sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
                message.body.split(ParagraphBreak).forEach { paragraph ->
                    val trimmed = paragraph.trim()
                    if (trimmed.isNotEmpty()) {
                        Text(
                            text = linkify(trimmed, accent = hue),
                            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (images.isNotEmpty()) {
                ImageGallery(
                    attachments = images,
                    accent = hue,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fadeUpOnAppear(delayMs = 220),
                )
            }

            if (nonImages.isNotEmpty()) {
                AttachmentsList(
                    attachments = nonImages,
                    accent = hue,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fadeUpOnAppear(delayMs = 280),
                )
            }

            if (message.starred) {
                SavedNote(modifier = Modifier.padding(top = 24.dp))
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StarToggle(starred: Boolean, onToggle: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val tonal = accent.copy(alpha = 0.16f).compositeOver(MaterialTheme.melon.surface.card)
    val label = stringResource(
        if (starred) R.string.messages_unsave_a11y else R.string.messages_save_a11y,
    )
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (starred) tonal else Color.Transparent)
            .clickable(onClickLabel = label, onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (starred) Icons.Filled.Star else Icons.Filled.StarBorder,
            contentDescription = label,
            tint = if (starred) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun SenderHeaderCard(message: Message, hue: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(hue.copy(alpha = 0.12f).compositeOver(MaterialTheme.melon.surface.card))
            .border(1.dp, hue.copy(alpha = 0.24f), shape)
            .padding(18.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(hue.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = originIcon(message.origin),
                contentDescription = null,
                tint = hue,
                modifier = Modifier.size(26.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.sender.role.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.88.sp,
                ),
                color = hue,
            )
            Text(
                text = message.sender.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 21.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.42).sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = stringResource(originKindRes(message.origin)),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun SavedNote(modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val tonal = accent.copy(alpha = 0.16f).compositeOver(MaterialTheme.melon.surface.card)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(tonal)
            .padding(horizontal = 15.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Bookmark,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.messages_saved_note),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = accent,
        )
    }
}

// Paragraphs are separated by blank lines in the upstream body text.
private val ParagraphBreak = Regex("\\n\\s*\\n")

// Build an annotated body where URL-like tokens are tinted in the category
// accent color, underlined, and clickable. `Text` routes `LinkAnnotation.Url`
// clicks through the ambient `UriHandler`, which hands off to the system
// browser.
private val UrlPattern = Regex(
    "(https?://[^\\s]+|www\\.[^\\s]+|[a-z0-9.-]+\\.(?:br|com|org|edu|net|io)/[^\\s]*)",
    RegexOption.IGNORE_CASE,
)

private fun linkify(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    val linkStyle = TextLinkStyles(
        style = SpanStyle(color = accent, textDecoration = TextDecoration.Underline),
    )
    var cursor = 0
    UrlPattern.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        val raw = match.value
        val href = when {
            raw.startsWith("http://", ignoreCase = true) -> raw
            raw.startsWith("https://", ignoreCase = true) -> raw
            else -> "https://$raw"
        }
        withLink(LinkAnnotation.Url(url = href, styles = linkStyle)) {
            append(raw)
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

@Composable
private fun ImageGallery(
    attachments: List<MessageAttachment>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (attachments.size == 1) {
            AttachmentTile(attachment = attachments.first(), accent = accent)
        } else {
            attachments.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    pair.forEach { att ->
                        Box(modifier = Modifier.weight(1f)) {
                            AttachmentTile(attachment = att, accent = accent)
                        }
                    }
                    if (pair.size == 1) Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AttachmentsList(
    attachments: List<MessageAttachment>,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.messages_attachments_section_format, attachments.size),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        attachments.forEach { AttachmentTile(attachment = it, accent = accent) }
    }
}

@Preview
@Composable
private fun MessageDetailScreenPreview() {
    MelonTheme {
        val roles = rememberMessageRoleStrings()
        val seed = MessagesFixtures.items[1]
        val detail = MessagesFixtures.detailById[seed.id]
        val message = detail?.toUi(roles) ?: seed.toUi(roles)
        MessageDetailScreen(
            message = message,
            onBack = {},
        )
    }
}
