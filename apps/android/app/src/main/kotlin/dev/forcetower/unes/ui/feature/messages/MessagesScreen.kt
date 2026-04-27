package dev.forcetower.unes.ui.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.components.FilterChipRow
import dev.forcetower.unes.ui.feature.messages.components.MessageRow
import java.time.LocalDateTime
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem as KmpMessageFeedItem

// Messages ("Mensagens") inbox — grouped by date bucket with filter chips at
// the top. Tapping a row pushes `MessageDetail` onto the Messages tab's back
// stack (see `ConnectedNavigator`), which keeps the floating tab bar visible
// and lets system back / predictive back pop it like a native nav stack.
//
// Mirrors `MessagesScreen` in `screens-messages.jsx` and `MessagesListView`
// on iOS. Driven by `MessagesViewModel`, which subscribes to the KMP inbox
// flow; the per-message detail flow is started by `MessageDetailRoute` on
// composition and torn down on dispose.
@Composable
internal fun MessagesScreen(
    onOpen: (id: String, seed: KmpMessageFeedItem) -> Unit,
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val vm: MessagesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    MessagesInbox(
        state = state,
        onIntent = vm::onIntent,
        onOpen = onOpen,
        bottomInset = bottomInset,
        modifier = modifier,
    )
}

@Composable
private fun MessagesInbox(
    state: MessagesUiState,
    onIntent: (MessagesIntent) -> Unit,
    onOpen: (id: String, seed: KmpMessageFeedItem) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val roles = rememberMessageRoleStrings()
    Box(modifier = modifier.fillMaxSize()) {
        InboxList(
            rawItems = state.rawItems,
            filter = state.filter,
            roles = roles,
            onFilterChange = { onIntent(MessagesIntent.SetFilter(it)) },
            onOpen = onOpen,
            bottomInset = bottomInset,
        )
    }
}

@Composable
private fun InboxList(
    rawItems: List<KmpMessageFeedItem>,
    filter: MessageFilter,
    roles: MessageRoleStrings,
    onFilterChange: (MessageFilter) -> Unit,
    onOpen: (String, KmpMessageFeedItem) -> Unit,
    bottomInset: Dp,
) {
    val surface = MaterialTheme.colorScheme.surface
    val seedById = remember(rawItems) { rawItems.associateBy { it.id } }
    val messages = remember(rawItems, roles) { rawItems.map { it.toUi(roles) } }

    val counts = remember(messages) {
        MessageFilter.entries.associateWith { f -> messages.count(f::matches) }
    }
    val unreadCount = remember(messages) { messages.count { it.unread } }
    val filtered = remember(messages, filter) { messages.filter(filter::matches) }
    val now = remember(messages) { LocalDateTime.now() }
    val buckets = remember(filtered, now) { groupByBucket(filtered, now) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(surface),
    ) {
        Box(modifier = Modifier.align(Alignment.TopCenter)) {
            AmbientMeshTop(surface = surface)
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
        ) {
            item("messages-header") {
                MessagesHeader(
                    unreadCount = unreadCount,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 20),
                )
            }
            item("messages-filter") {
                FilterChipRow(
                    active = filter,
                    counts = counts,
                    onChange = onFilterChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp)
                        .fadeUpOnAppear(delayMs = 100),
                )
            }

            if (buckets.isEmpty()) {
                item("messages-empty") { EmptyState() }
            } else {
                buckets.forEachIndexed { index, bucket ->
                    item(key = "bucket-${bucket.bucket.name}-header") {
                        BucketHeader(
                            label = stringResource(bucket.bucket.labelRes),
                            count = bucket.items.size,
                            modifier = Modifier.fadeUpOnAppear(delayMs = 180 + index * 60),
                        )
                    }
                    val rows = bucket.items
                    val lastIndex = rows.lastIndex
                    itemsIndexed(
                        items = rows,
                        key = { _, m -> m.id },
                    ) { i, m ->
                        BucketRow(
                            message = m,
                            isFirst = i == 0,
                            isLast = i == lastIndex,
                            onOpen = { msg ->
                                seedById[msg.id]?.let { seed -> onOpen(msg.id, seed) }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AmbientMeshTop(surface: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
    ) {
        Mesh(
            variant = MeshVariant.Warm,
            intensity = 0.45f,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.95f to surface,
                    ),
                ),
        )
    }
}

@Composable
private fun MessagesHeader(unreadCount: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.messages_eyebrow_inbox),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (unreadCount > 0) UnreadBadge(unreadCount)
        }
        Text(
            text = stringResource(R.string.messages_title),
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 32.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun UnreadBadge(count: Int) {
    val coral = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(coral.copy(alpha = 0.095f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(coral),
        )
        Text(
            text = stringResource(R.string.messages_unread_badge_format, count),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            ),
            color = coral,
        )
    }
}

// One LazyColumn item per message. The bucket "card" frame (rounded
// corners, side borders, top/bottom edges, inter-row dividers) is drawn
// per row via `drawBucketEdges` so adjacent rows visually compose into a
// single bordered card without needing a shared parent layout.
@Composable
private fun BucketRow(
    message: Message,
    isFirst: Boolean,
    isLast: Boolean,
    onOpen: (Message) -> Unit,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line
    val shape = remember(isFirst, isLast) { bucketRowShape(isFirst, isLast) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .clip(shape)
            .background(card)
            .drawBehind {
                drawBucketEdges(
                    isFirst = isFirst,
                    isLast = isLast,
                    cardLine = cardLine,
                    line = line,
                )
            },
    ) {
        MessageRow(message = message, onOpen = onOpen)
    }
}

private val BucketRowCornerRadius = 18.dp

private fun bucketRowShape(isFirst: Boolean, isLast: Boolean): Shape {
    val r = BucketRowCornerRadius
    return when {
        isFirst && isLast -> RoundedCornerShape(r)
        isFirst -> RoundedCornerShape(topStart = r, topEnd = r, bottomStart = 0.dp, bottomEnd = 0.dp)
        isLast -> RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = r, bottomEnd = r)
        else -> RectangleShape
    }
}

private fun DrawScope.drawBucketEdges(
    isFirst: Boolean,
    isLast: Boolean,
    cardLine: Color,
    line: Color,
) {
    val strokePx = 1.dp.toPx()
    val radiusPx = BucketRowCornerRadius.toPx()
    val w = size.width
    val h = size.height
    val inset = strokePx / 2f
    val stroke = Stroke(width = strokePx)

    drawLine(
        color = cardLine,
        start = Offset(inset, if (isFirst) radiusPx else 0f),
        end = Offset(inset, if (isLast) h - radiusPx else h),
        strokeWidth = strokePx,
    )
    drawLine(
        color = cardLine,
        start = Offset(w - inset, if (isFirst) radiusPx else 0f),
        end = Offset(w - inset, if (isLast) h - radiusPx else h),
        strokeWidth = strokePx,
    )

    val arcSize = Size(2f * radiusPx - strokePx, 2f * radiusPx - strokePx)

    if (isFirst) {
        val topPath = Path().apply {
            moveTo(inset, radiusPx)
            arcTo(
                rect = Rect(offset = Offset(inset, inset), size = arcSize),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(w - radiusPx, inset)
            arcTo(
                rect = Rect(offset = Offset(w - 2f * radiusPx + inset, inset), size = arcSize),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        }
        drawPath(topPath, color = cardLine, style = stroke)
    }

    if (isLast) {
        val bottomPath = Path().apply {
            moveTo(w - inset, h - radiusPx)
            arcTo(
                rect = Rect(
                    offset = Offset(w - 2f * radiusPx + inset, h - 2f * radiusPx + inset),
                    size = arcSize,
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
            lineTo(radiusPx, h - inset)
            arcTo(
                rect = Rect(offset = Offset(inset, h - 2f * radiusPx + inset), size = arcSize),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false,
            )
        }
        drawPath(bottomPath, color = cardLine, style = stroke)
    }

    if (!isLast) {
        drawLine(
            color = line,
            start = Offset(0f, h - inset),
            end = Offset(w, h - inset),
            strokeWidth = strokePx,
        )
    }
}

@Composable
private fun BucketHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 18.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.4.sp,
            ),
            color = ink4,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
            ),
            color = ink4.copy(alpha = 0.55f),
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f)
                .height(1.dp)
                .background(line.copy(alpha = 0.6f)),
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.messages_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal data class BucketGroup(val bucket: MessageBucket, val items: List<Message>)

private fun groupByBucket(messages: List<Message>, now: LocalDateTime): List<BucketGroup> {
    val map = LinkedHashMap<MessageBucket, MutableList<Message>>()
    messages.forEach { m ->
        map.getOrPut(bucketOf(m.receivedAt, now)) { mutableListOf() }.add(m)
    }
    return MessageBucket.entries.mapNotNull { b ->
        map[b]?.let { items -> BucketGroup(b, items) }
    }
}

@Preview
@Composable
private fun MessagesScreenPreview() {
    MelonTheme {
        MessagesInbox(
            state = MessagesFixtures.previewState(),
            onIntent = {},
            onOpen = { _, _ -> },
            bottomInset = 0.dp,
        )
    }
}
