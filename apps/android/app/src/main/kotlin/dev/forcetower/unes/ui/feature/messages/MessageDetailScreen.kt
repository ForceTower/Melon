package dev.forcetower.unes.ui.feature.messages

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.components.AttachmentTile
import dev.forcetower.unes.ui.feature.messages.components.OriginSwatch

// Full message detail — sender card, optional serif subject, formatted body
// with linkified URLs, image gallery, and the attachments list.
//
// Mirrors `MessageDetailScreen` in `screens-message-detail.jsx` and
// `MessageDetailView` on iOS.
@Composable
internal fun MessageDetailScreen(
    message: Message,
    onBack: () -> Unit,
    bottomInset: Dp = 0.dp,
    onAppear: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val meta = originMeta(message)
    val surface = MaterialTheme.colorScheme.surface
    val images = message.attachments.filter { it.kind == MessageAttachmentKind.Image }
    val nonImages = message.attachments.filter { it.kind != MessageAttachmentKind.Image }

    LaunchedEffect(message.id) {
        onAppear?.invoke()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Origin-tinted ambient glow at the top.
        AmbientGlow(accent = meta.color, surface = surface)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            BackButton(
                onBack = onBack,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp),
            )

            SenderCard(
                message = message,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 18.dp)
                    .fadeUpOnAppear(delayMs = 40),
            )

            SubjectAndTimestamp(
                message = message,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp)
                    .fadeUpOnAppear(delayMs = 120),
            )

            BodyText(
                message = message,
                accent = meta.color,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp)
                    .fadeUpOnAppear(delayMs = 200),
            )

            if (images.isNotEmpty()) {
                ImageGallery(
                    attachments = images,
                    accent = meta.color,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .fadeUpOnAppear(delayMs = 280),
                )
            }

            if (nonImages.isNotEmpty()) {
                AttachmentsList(
                    attachments = nonImages,
                    accent = meta.color,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 20.dp)
                        .fadeUpOnAppear(delayMs = 340),
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun AmbientGlow(accent: Color, surface: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
    ) {
        // Radial-ish glow centered at the top, faded to transparent.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = 0.165f), Color.Transparent),
                        radius = 720f,
                    ),
                ),
        )
        // Linear fade to the surface color so the bottom edge ends cleanly.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to surface,
                    ),
                ),
        )
    }
}

@Composable
private fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val description = stringResource(R.string.messages_back_label)
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(card)
            .border(1.dp, cardLine, CircleShape)
            .clickable(onClick = onBack)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val sx = w / 14f
            val sy = h / 14f
            val stroke = Stroke(width = 1.5f * density, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val path = Path().apply {
                moveTo(8.5f * sx, 3f * sy)
                lineTo(4.5f * sx, 7f * sy)
                lineTo(8.5f * sx, 11f * sy)
            }
            drawPath(path, color = ink, style = stroke)
        }
    }
}

@Composable
private fun SenderCard(message: Message, modifier: Modifier = Modifier) {
    val meta = originMeta(message)
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink = MaterialTheme.colorScheme.onBackground
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(card)
            .border(1.dp, cardLine, RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        OriginSwatch(message = message, size = 44.dp)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = senderKindLabel(message = message, meta = meta),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                ),
                color = meta.color,
                modifier = Modifier.padding(bottom = 2.dp),
            )
            Text(
                text = message.sender.name,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.15).sp,
                ),
                color = ink,
            )
            Text(
                text = message.sender.role,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = ink3,
            )
        }

        StarToggle(starred = message.starred)
    }
}

@Composable
private fun senderKindLabel(message: Message, meta: MessageOriginMeta): String {
    val kind = stringResource(meta.kindRes)
    return if (message.origin == MessageOrigin.Direct) {
        stringResource(R.string.messages_origin_kind_direct_for_you, kind)
    } else {
        kind.uppercase()
    }
}

@Composable
private fun StarToggle(starred: Boolean) {
    val cardLine = MaterialTheme.melon.surface.cardLine
    val ink3 = MaterialTheme.colorScheme.onSurfaceVariant
    val starOn = StarColor
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, cardLine, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(13.dp)) {
            val w = size.width
            val h = size.height
            val sx = w / 13f
            val sy = h / 13f
            val color = if (starred) starOn else ink3
            val path = Path().apply {
                moveTo(6.5f * sx, 1.3f * sy)
                lineTo(8.05f * sx, 4.5f * sy)
                lineTo(11.55f * sx, 4.85f * sy)
                lineTo(8.95f * sx, 7.25f * sy)
                lineTo(9.65f * sx, 10.6f * sy)
                lineTo(6.5f * sx, 9f * sy)
                lineTo(3.35f * sx, 10.6f * sy)
                lineTo(4.05f * sx, 7.25f * sy)
                lineTo(1.45f * sx, 4.85f * sy)
                lineTo(4.95f * sx, 4.5f * sy)
                close()
            }
            if (starred) {
                drawPath(path, color = color)
            } else {
                drawPath(path, color = color, style = Stroke(width = 1.2f * density, join = StrokeJoin.Round))
            }
        }
    }
}

@Composable
private fun SubjectAndTimestamp(message: Message, modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!message.subject.isNullOrBlank()) {
            Text(
                text = message.subject,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    lineHeight = 31.sp,
                    letterSpacing = (-0.52).sp,
                ),
                color = ink,
            )
        }
        Text(
            text = fullTime(message.receivedAt),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
            ),
            color = ink4,
        )
    }
}

@Composable
private fun BodyText(message: Message, accent: Color, modifier: Modifier = Modifier) {
    val hasSubject = message.subject != null
    val ink = MaterialTheme.colorScheme.onBackground
    val ink2 = MaterialTheme.colorScheme.onSurface
    Text(
        text = linkify(message.body, accent = accent),
        style = MaterialTheme.typography.bodyLarge.copy(
            fontSize = if (hasSubject) 14.sp else 15.sp,
            lineHeight = if (hasSubject) 22.sp else 24.sp,
            fontWeight = FontWeight.Normal,
        ),
        color = if (hasSubject) ink2 else ink,
        modifier = modifier.fillMaxWidth(),
    )
}

// Build an annotated body where URL-like tokens are tinted in the origin
// accent color and underlined. URLs are not actually clickable here — the
// tokens are styled the same way iOS shows them, but the design surface
// doesn't open external browsers from the message yet.
private val UrlPattern = Regex(
    "(https?://[^\\s]+|www\\.[^\\s]+|[a-z0-9.-]+\\.(?:br|com|org|edu|net|io)/[^\\s]*)",
    RegexOption.IGNORE_CASE,
)

private fun linkify(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    UrlPattern.findAll(text).forEach { match ->
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.Underline)) {
            append(match.value)
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
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(start = 4.dp),
        )
        attachments.forEach { AttachmentTile(attachment = it, accent = accent) }
    }
}

// Keep the iOS/JSX star-on color as-is — see `MessageRow.StarColor`.
private val StarColor = Color(0xFFD9852E)

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
