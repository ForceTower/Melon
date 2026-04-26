package dev.forcetower.unes.ui.feature.messages

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
// the top. Tapping a row swaps the screen for `MessageDetailScreen`, which
// keeps the floating tab bar visible (the JSX prototype's pattern).
//
// Mirrors `MessagesScreen` in `screens-messages.jsx` and `MessagesListView`
// on iOS. Driven by `MessagesViewModel`, which subscribes to the KMP inbox
// flow and starts a child detail subscription whenever a row is opened.
@Composable
internal fun MessagesScreen(
    bottomInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val vm: MessagesViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    MessagesContent(
        state = state,
        onIntent = vm::onIntent,
        bottomInset = bottomInset,
        modifier = modifier,
    )
}

@Composable
private fun MessagesContent(
    state: MessagesUiState,
    onIntent: (MessagesIntent) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val roles = rememberMessageRoleStrings()
    Box(modifier = modifier.fillMaxSize()) {
        val openSeed = state.openSeed
        val openId = state.openMessageId
        if (openId != null) {
            val rendered = state.openDetail?.toUi(roles) ?: openSeed?.toUi(roles)
            if (rendered != null) {
                MessageDetailScreen(
                    message = rendered,
                    onBack = { onIntent(MessagesIntent.CloseMessage) },
                    onAppear = { onIntent(MessagesIntent.MarkRead(rendered.id)) },
                    bottomInset = bottomInset,
                )
            }
        } else {
            InboxList(
                rawItems = state.rawItems,
                filter = state.filter,
                roles = roles,
                onFilterChange = { onIntent(MessagesIntent.SetFilter(it)) },
                onOpen = { id, seed -> onIntent(MessagesIntent.OpenMessage(id, seed)) },
                bottomInset = bottomInset,
            )
        }
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
    val filtered = messages.filter(filter::matches)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            MessagesHeader(
                unreadCount = unreadCount,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            FilterChipRow(
                active = filter,
                counts = counts,
                onChange = onFilterChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 6.dp)
                    .fadeUpOnAppear(delayMs = 100),
            )

            if (buckets.isEmpty()) {
                EmptyState()
            } else {
                buckets.forEachIndexed { index, bucket ->
                    BucketCard(
                        bucket = bucket,
                        onOpen = { message ->
                            seedById[message.id]?.let { seed -> onOpen(message.id, seed) }
                        },
                        modifier = Modifier.fadeUpOnAppear(delayMs = 180 + index * 60),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
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

@Composable
private fun BucketCard(
    bucket: BucketGroup,
    onOpen: (Message) -> Unit,
    modifier: Modifier = Modifier,
) {
    val card = MaterialTheme.melon.surface.card
    val cardLine = MaterialTheme.melon.surface.cardLine
    val line = MaterialTheme.melon.surface.line

    Column(modifier = modifier) {
        BucketHeader(label = stringResource(bucket.bucket.labelRes), count = bucket.items.size)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(card)
                .border(1.dp, cardLine, RoundedCornerShape(18.dp)),
        ) {
            bucket.items.forEachIndexed { i, m ->
                MessageRow(message = m, onOpen = onOpen)
                if (i < bucket.items.size - 1) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(line),
                    )
                }
            }
        }
    }
}

@Composable
private fun BucketHeader(label: String, count: Int) {
    val ink4 = MaterialTheme.colorScheme.outlineVariant
    val line = MaterialTheme.melon.surface.line
    Row(
        modifier = Modifier
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
        MessagesContent(
            state = MessagesFixtures.previewState(),
            onIntent = {},
            bottomInset = 0.dp,
        )
    }
}
