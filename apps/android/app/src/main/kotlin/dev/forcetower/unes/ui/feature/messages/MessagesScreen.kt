package dev.forcetower.unes.ui.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.messages.components.FilterChipRow
import dev.forcetower.unes.ui.feature.messages.components.MessageRow
import java.time.LocalDateTime
import dev.forcetower.melon.feature.messages.domain.model.MessageFeedItem as KmpMessageFeedItem

// Messages ("Mensagens") inbox — 2026 redesign (dc project `UNES Mensagens -
// Android`): M3 large-style app bar with the unread/total sub-line, the
// tonal unread hero (big count, per-category segmented bar + legend, and the
// mark-all-read tonal button), M3 filter chips, and the date-bucketed list of
// tonal-avatar rows. Tapping a row pushes `MessageDetail` onto the Messages
// tab's back stack (see `ConnectedNavigator`).
//
// Driven by `MessagesViewModel`, which subscribes to the KMP inbox flow; the
// per-message detail flow is started by `MessageDetailRoute` on composition
// and torn down on dispose.
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
    val seedById = remember(state.rawItems) { state.rawItems.associateBy { it.id } }
    val messages = remember(state.rawItems, roles) { state.rawItems.map { it.toUi(roles) } }

    val counts = remember(messages) {
        MessageFilter.entries.associateWith { f -> messages.count(f::matches) }
    }
    val unreadCount = remember(messages) { messages.count { it.unread } }
    val categoryCounts = remember(messages) {
        MessageCategory.entries.associateWith { c -> messages.count { it.category == c } }
    }
    val filtered = remember(messages, state.filter) { messages.filter(state.filter::matches) }
    val now = remember(messages) { LocalDateTime.now() }
    val buckets = remember(filtered, now) { groupByBucket(filtered, now) }

    val listState = rememberLazyListState()
    val scrolled by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0
        }
    }

    // The title header stays pinned (matching iOS); the hero, chips, and the
    // bucketed list scroll beneath it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Header(
            unreadCount = unreadCount,
            total = messages.size,
            modifier = Modifier.fadeUpOnAppear(delayMs = 60, fromOffset = (-10).dp),
        )
        PinnedHeaderHairline(scrolled = scrolled)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
        ) {
            item("messages-hero") {
                UnreadHeroCard(
                    unreadCount = unreadCount,
                    total = messages.size,
                    categoryCounts = categoryCounts,
                    onMarkAllRead = { onIntent(MessagesIntent.MarkAllRead) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fadeUpOnAppear(delayMs = 120),
                )
            }
            item("messages-filter") {
                FilterChipRow(
                    active = state.filter,
                    counts = counts,
                    onChange = { onIntent(MessagesIntent.SetFilter(it)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 8.dp)
                        .fadeUpOnAppear(delayMs = 160, fromOffset = (-8).dp),
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
                            modifier = Modifier.fadeUpOnAppear(delayMs = 200 + index * 60),
                        )
                    }
                    itemsIndexed(
                        items = bucket.items,
                        key = { _, m -> m.id },
                    ) { _, message ->
                        MessageRow(
                            message = message,
                            onOpen = { msg ->
                                seedById[msg.id]?.let { seed -> onOpen(msg.id, seed) }
                            },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 3.dp)
                                .fadeUpOnAppear(delayMs = 240 + index * 60),
                        )
                    }
                }
            }
        }
    }
}

// ══════════ Large app bar ══════════

@Composable
private fun Header(unreadCount: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 20.dp)
            .padding(top = 20.dp, bottom = 14.dp),
    ) {
        Text(
            text = stringResource(R.string.messages_title),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 34.sp,
                lineHeight = 35.sp,
                letterSpacing = (-0.85).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.messages_unread_count_format, unreadCount),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Dot()
            Text(
                text = stringResource(R.string.messages_total_count_format, total),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun Dot() {
    Box(
        modifier = Modifier
            .size(3.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant),
    )
}

// ══════════ Unread tonal hero ══════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnreadHeroCard(
    unreadCount: Int,
    total: Int,
    categoryCounts: Map<MessageCategory, Int>,
    onMarkAllRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.melon.surface.line, shape)
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.messages_hero_unread_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.54.sp,
                    ),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = unreadCount.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            lineHeight = 48.sp,
                            letterSpacing = (-2.08).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = stringResource(R.string.messages_hero_new_suffix),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = total.toString(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 30.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.9).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.messages_hero_total_label),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }

        CategorySegmentedBar(
            categoryCounts = categoryCounts,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp, bottom = 14.dp),
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MessageCategory.entries.forEach { category ->
                LegendEntry(category = category, count = categoryCounts[category] ?: 0)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .height(1.dp)
                .background(MaterialTheme.melon.surface.line),
        )

        MarkAllReadButton(onClick = onMarkAllRead)
    }
}

@Composable
private fun CategorySegmentedBar(
    categoryCounts: Map<MessageCategory, Int>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.height(8.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        MessageCategory.entries.forEach { category ->
            val count = categoryCounts[category] ?: 0
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .weight(count.toFloat())
                        .widthIn(min = 6.dp)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(categoryColor(category)),
                )
            }
        }
    }
}

@Composable
private fun LegendEntry(category: MessageCategory, count: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(categoryColor(category)),
        )
        Text(
            text = stringResource(category.labelRes),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
            ),
            color = MaterialTheme.colorScheme.outlineVariant,
        )
    }
}

@Composable
private fun MarkAllReadButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val tonal = accent.copy(alpha = 0.16f).compositeOver(MaterialTheme.melon.surface.card)
    val a11y = stringResource(R.string.messages_mark_all_read_a11y)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(tonal)
            .border(1.dp, accent.copy(alpha = 0.32f), CircleShape)
            .clickable(onClickLabel = a11y, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.DoneAll,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = stringResource(R.string.messages_mark_all_read),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = accent,
        )
    }
}

// ══════════ Date buckets ══════════

@Composable
private fun BucketHeader(label: String, count: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.melon.surface.line),
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.MarkEmailRead,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(44.dp),
        )
        Text(
            text = stringResource(R.string.messages_empty_title),
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.messages_empty_body),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                lineHeight = 18.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
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
