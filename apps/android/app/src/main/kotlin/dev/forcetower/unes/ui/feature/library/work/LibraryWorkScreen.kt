package dev.forcetower.unes.ui.feature.library.work

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.melon.feature.library.domain.model.LibraryAvailability
import dev.forcetower.melon.feature.library.domain.model.LibraryBranch
import dev.forcetower.melon.feature.library.domain.model.LibraryCopy
import dev.forcetower.melon.feature.library.domain.model.LibraryCopyStatus
import dev.forcetower.melon.feature.library.domain.model.LibraryReading
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchScope
import dev.forcetower.melon.feature.library.domain.model.LibrarySearchTerm
import dev.forcetower.melon.feature.library.domain.model.LibraryWork
import dev.forcetower.melon.feature.library.domain.model.LibraryYear
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.theme.MelonTheme
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.ui.feature.library.components.LibraryBackButton
import dev.forcetower.unes.ui.feature.library.components.LibraryCard
import dev.forcetower.unes.ui.feature.library.components.LibraryFreshnessRow
import dev.forcetower.unes.ui.feature.library.components.LibraryInfoNote
import dev.forcetower.unes.ui.feature.library.components.LibrarySectionLabel
import dev.forcetower.unes.ui.feature.library.components.LibraryShimmerBar
import dev.forcetower.unes.ui.feature.library.components.LibraryTypeTag
import dev.forcetower.unes.ui.feature.library.components.color
import dev.forcetower.unes.ui.feature.library.components.verdictTone
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.forcetower.unes.ui.feature.library.LibraryViewModel
import dev.forcetower.unes.ui.feature.library.LibraryIntent
import dev.forcetower.unes.ui.feature.library.formatLibraryAgo
import dev.forcetower.unes.ui.feature.library.formatLibraryDate
import dev.forcetower.unes.ui.feature.library.formatLibraryYear
import dev.forcetower.unes.ui.feature.library.libraryPreviewWork

// Work detail (dc `BibliotecaScreen` "detalhe" scenarios): the availability
// answer card, where-on-the-shelf with the copyable call number, copies
// grouped per branch+volume, the collapsible catalogue record, subject/author
// jumps into new searches, and the ABNT reference. The record itself arrives
// in-memory from the shared ViewModel; only the availability consultation
// hits the network here.
@Composable
internal fun LibraryWorkScreen(
    workId: String,
    seedTitle: String?,
    onBack: () -> Unit,
    onOpenResults: (query: String, scopeWire: String?, sessionId: Int) -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: LibraryViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()

    val work = state.works[workId]
    LaunchedEffect(workId) { vm.onIntent(LibraryIntent.EnsureReading(workId)) }

    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Clock.System.now()
        }
    }

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 40 } }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val copyToast: (String, String) -> Unit = { text, message ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(null, text))
        scope.launch { snackbar.showSnackbar(message) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 8.dp)
                    .height(48.dp),
            ) {
                LibraryBackButton(onBack = onBack)
                Text(
                    text = (work?.parsedTitle?.title ?: seedTitle).orEmpty(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.32).sp,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .alpha(if (scrolled) 1f else 0f),
                )
            }
            PinnedHeaderHairline(scrolled = scrolled)

            if (work == null) {
                // Only reachable after process death — the record never rode
                // the route, and there is no per-work fetch in the contract.
                MissingRecordState(seedTitle = seedTitle, onBack = onBack)
            } else {
                WorkContent(
                    work = work,
                    reading = state.readings[workId],
                    copies = state.liveCopies[workId] ?: work.copies,
                    refreshing = state.refreshing,
                    now = now,
                    scrollState = scrollState,
                    onRefresh = { vm.onIntent(LibraryIntent.RefreshReadings(listOf(workId))) },
                    onSearch = { query, searchScope ->
                        val sessionId = vm.startSearch(
                            listOf(LibrarySearchTerm(query, searchScope)),
                        )
                        if (sessionId != 0) {
                            onOpenResults(query, searchScope.wire, sessionId)
                        }
                    },
                    onCopy = copyToast,
                    bottomInset = bottomInset,
                )
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomInset + 24.dp),
        )
    }
}

@Composable
private fun WorkContent(
    work: LibraryWork,
    reading: LibraryReading?,
    copies: List<LibraryCopy>,
    refreshing: Boolean,
    now: Instant,
    scrollState: androidx.compose.foundation.ScrollState,
    onRefresh: () -> Unit,
    onSearch: (String, LibrarySearchScope) -> Unit,
    onCopy: (text: String, message: String) -> Unit,
    bottomInset: Dp,
) {
    val effective = work.copy(copies = copies)
    val availability = effective.availability(now)
    var recordOpen by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = bottomInset + 48.dp),
    ) {
        // Hero
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
            LibraryTypeTag(work = work)
            Text(
                text = work.parsedTitle.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 26.sp,
                    letterSpacing = (-0.78).sp,
                    lineHeight = 30.sp,
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 8.dp),
            )
            work.parsedTitle.subtitle?.let { subtitle ->
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        // Author chips → new author-scoped search.
        if (work.authors.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp),
            ) {
                work.authors.forEach { author ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.melon.surface.card)
                            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(8.dp))
                            .clickable { onSearch(author, LibrarySearchScope.Author) }
                            .padding(horizontal = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            text = author,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        // Meta line: year · edition · language.
        val year = work.year
        val meta = listOfNotNull(
            (year as? LibraryYear.Year)?.text,
            work.edition,
            work.language?.let { if (it == "Portuguese") stringResource(R.string.library_language_portuguese) else it },
            (year as? LibraryYear.Illegible)?.let {
                stringResource(R.string.library_detail_meta_year_invalid, it.text)
            },
        )
        Text(
            text = meta.joinToString(" · ")
                .ifEmpty { stringResource(R.string.library_detail_no_meta) },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp),
        )

        // The answer card.
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp),
        ) {
            AnswerCard(
                work = effective,
                availability = availability,
                reading = reading,
                refreshing = refreshing,
                now = now,
                onRefresh = onRefresh,
            )
        }

        // Where on the shelf.
        LibrarySectionLabel(
            text = stringResource(R.string.library_detail_where_label),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
        ) {
            LibraryCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text(
                        text = stringResource(R.string.library_detail_call_label).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.9.sp,
                        ),
                        color = MaterialTheme.colorScheme.outline,
                    )
                    val callCopied = stringResource(R.string.library_toast_call_copied)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onCopy(work.callNumber, callCopied) },
                    ) {
                        Text(
                            text = work.callNumber,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 26.sp,
                                letterSpacing = (-0.78).sp,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = stringResource(R.string.library_toast_call_copied),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(top = 16.dp),
                    ) {
                        availability.branches.forEach { branch ->
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(
                                    imageVector = if (branch.branch.isNear) {
                                        Icons.AutoMirrored.Filled.LibraryBooks
                                    } else {
                                        Icons.Filled.LocationOn
                                    },
                                    contentDescription = null,
                                    tint = if (branch.branch.isNear) {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    } else {
                                        MaterialTheme.melon.status.warn
                                    },
                                    modifier = Modifier.size(19.dp),
                                )
                                Column {
                                    Text(
                                        text = branch.areas.filter { it.isNotEmpty() }
                                            .joinToString(" · ")
                                            .ifEmpty {
                                                stringResource(R.string.library_detail_general_area)
                                            },
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = (-0.14).sp,
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    Text(
                                        text = if (branch.branch.isNear) {
                                            stringResource(
                                                R.string.library_detail_place,
                                                branch.branch.name,
                                                branch.branch.campus.orEmpty(),
                                            )
                                        } else {
                                            stringResource(
                                                R.string.library_detail_place_far,
                                                branch.branch.name,
                                                branch.branch.campus.orEmpty(),
                                            )
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 12.5.sp,
                                        ),
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Copies per branch + volume.
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 26.dp),
        ) {
            LibrarySectionLabel(
                text = stringResource(R.string.library_detail_copies_label),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (reading == LibraryReading.Unavailable) {
                    stringResource(R.string.library_detail_copies_registered, copies.size)
                } else {
                    stringResource(R.string.library_detail_copies_count, availability.total)
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MaterialTheme.colorScheme.outline,
            )
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
        ) {
            groupCopies(effective).forEach { group ->
                CopyGroupCard(group = group, down = reading == LibraryReading.Unavailable, now = now)
            }
        }

        if (availability.missing > 0 && reading != LibraryReading.Unavailable) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp),
            ) {
                LibraryInfoNote(
                    text = stringResource(
                        R.string.library_detail_missing_note,
                        copies.size,
                        availability.missing,
                        availability.available,
                        availability.total,
                    ),
                    icon = Icons.Filled.Report,
                )
            }
        }

        // Catalogue record (ficha).
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp)
                .padding(top = 28.dp),
        ) {
            LibrarySectionLabel(
                text = stringResource(R.string.library_detail_ficha_label),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { recordOpen = !recordOpen }) {
                Text(
                    text = stringResource(
                        if (recordOpen) {
                            R.string.library_detail_ficha_hide
                        } else {
                            R.string.library_detail_ficha_show
                        },
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (recordOpen) 180f else 0f),
                )
            }
        }
        if (recordOpen && work.record.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 8.dp),
            ) {
                LibraryCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        work.record.forEachIndexed { index, field ->
                            if (index > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(
                                            MaterialTheme.colorScheme.outlineVariant
                                                .copy(alpha = 0.5f),
                                        ),
                                )
                            }
                            Column(
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 12.dp,
                                ),
                            ) {
                                Text(
                                    text = field.label.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.7.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.outline,
                                )
                                Text(
                                    text = field.value,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(top = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Subjects → new subject-scoped search.
        if (work.subjects.isNotEmpty()) {
            LibrarySectionLabel(
                text = stringResource(R.string.library_detail_subjects_label),
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(top = 28.dp),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(top = 12.dp),
            ) {
                work.subjects.forEach { subject ->
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.melon.surface.card)
                            .border(1.dp, MaterialTheme.melon.surface.line, RoundedCornerShape(8.dp))
                            .clickable { onSearch(subject, LibrarySearchScope.Subject) }
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        // ABNT reference.
        LibrarySectionLabel(
            text = stringResource(R.string.library_detail_reference_label),
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 28.dp),
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 12.dp),
        ) {
            val reference = work.reference
            if (reference != null) {
                val plain = reference.replace("**", "")
                val copied = stringResource(R.string.library_toast_reference_copied)
                LibraryCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                        Text(
                            text = boldMarkdown(reference),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                lineHeight = 21.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        TextButton(
                            onClick = { onCopy(plain, copied) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(text = stringResource(R.string.library_detail_reference_copy))
                        }
                    }
                }
            } else {
                LibraryInfoNote(
                    text = stringResource(R.string.library_detail_reference_missing),
                    icon = Icons.Filled.Info,
                )
            }
        }

        // Identifier chips.
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 18.dp),
        ) {
            val isbn = work.isbn
            if (isbn?.value != null) {
                val toast = stringResource(R.string.library_toast_isbn_copied)
                IdChip(
                    label = if (isbn.value?.length == 13) {
                        stringResource(R.string.library_detail_id_isbn)
                    } else {
                        stringResource(R.string.library_detail_id_isbn10)
                    },
                    value = isbn.pretty ?: isbn.value.orEmpty(),
                    onTap = { onCopy(isbn.value.orEmpty(), toast) },
                )
            } else if (isbn?.note != null) {
                val toast = stringResource(R.string.library_toast_registro_copied)
                IdChip(
                    label = stringResource(R.string.library_detail_id_registro),
                    value = isbn.note.orEmpty(),
                    onTap = { onCopy(isbn.note.orEmpty(), toast) },
                )
            }
            val acervoToast = stringResource(R.string.library_toast_acervo_copied)
            IdChip(
                label = stringResource(R.string.library_detail_id_acervo),
                value = work.id,
                onTap = { onCopy(work.id, acervoToast) },
            )
        }

        // The honest footer about the raw record.
        Text(
            text = stringResource(R.string.library_detail_raw_note, work.rawTitle) +
                (
                    work.parsedTitle.junkYear?.let {
                        stringResource(R.string.library_detail_raw_note_junk, it)
                    } ?: ""
                    ),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp),
        )
    }
}

// The verdict card: skeleton → down / verdict + sub + stale note, with the
// freshness stamp and Atualizar pinned to its footer.
@Composable
private fun AnswerCard(
    work: LibraryWork,
    availability: LibraryAvailability,
    reading: LibraryReading?,
    refreshing: Boolean,
    now: Instant,
    onRefresh: () -> Unit,
) {
    LibraryCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 20) {
        Column(modifier = Modifier.padding(18.dp)) {
            when (reading) {
                null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LibraryShimmerBar(width = 180, height = 24)
                    LibraryShimmerBar(width = 124, height = 12)
                }
                LibraryReading.Unavailable -> Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = MaterialTheme.melon.status.bad,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            text = stringResource(R.string.library_detail_answer_down_title),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp,
                            ),
                            color = MaterialTheme.melon.status.bad,
                        )
                    }
                    Text(
                        text = stringResource(R.string.library_detail_answer_down_body),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
                is LibraryReading.Fresh, is LibraryReading.Stale -> {
                    val stale = reading is LibraryReading.Stale
                    val tone = if (stale) {
                        MaterialTheme.melon.status.warn
                    } else {
                        availability.verdictTone().color()
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(tone),
                        )
                        Text(
                            text = when (availability.verdict) {
                                LibraryAvailability.Verdict.Available -> pluralStringResource(
                                    R.plurals.library_detail_free,
                                    availability.available,
                                    availability.available,
                                    availability.total,
                                )
                                LibraryAvailability.Verdict.AllOnLoan ->
                                    stringResource(R.string.library_availability_none)
                                LibraryAvailability.Verdict.LocalUseOnly ->
                                    stringResource(R.string.library_availability_local)
                            },
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.69).sp,
                            ),
                            color = tone,
                        )
                    }
                    Text(
                        text = when (availability.verdict) {
                            LibraryAvailability.Verdict.Available ->
                                if (availability.hasNearAvailable) {
                                    stringResource(R.string.library_detail_near)
                                } else {
                                    stringResource(
                                        R.string.library_detail_far,
                                        availability.branches
                                            .firstOrNull { it.available > 0 }
                                            ?.branch?.campus
                                            ?: availability.branches.firstOrNull()
                                                ?.branch?.campus
                                            ?: "",
                                    )
                                }
                            LibraryAvailability.Verdict.LocalUseOnly ->
                                stringResource(R.string.library_detail_local_body)
                            LibraryAvailability.Verdict.AllOnLoan ->
                                availability.nextDue?.let {
                                    stringResource(
                                        R.string.library_detail_all_loaned_next,
                                        formatLibraryDate(it),
                                    )
                                } ?: stringResource(R.string.library_detail_all_loaned_never)
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 9.dp),
                    )
                    if (stale) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .padding(top = 14.dp)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.melon.status.warn.copy(alpha = 0.14f))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.HourglassTop,
                                contentDescription = null,
                                tint = MaterialTheme.melon.status.warn,
                                modifier = Modifier.size(19.dp),
                            )
                            Text(
                                text = stringResource(
                                    R.string.library_detail_stale_note,
                                    formatLibraryAgo(reading.checkedAt, now),
                                ),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.5.sp,
                                    lineHeight = 18.sp,
                                ),
                                color = MaterialTheme.melon.status.warn,
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            )
            LibraryFreshnessRow(
                reading = reading,
                checking = refreshing,
                now = now,
                onRefresh = onRefresh,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

// Copies aggregated by branch + per-copy call number: a single title can
// carry 122 physical copies — a flat list of 122 rows is useless.
private data class CopyGroup(
    val branch: LibraryBranch,
    val callNumber: String,
    val area: String,
    val available: Int,
    val missing: Int,
    val loans: List<Instant?>,
    val localNotes: List<String>,
) {
    val total: Int get() = available + loans.size + localNotes.size
}

private fun groupCopies(work: LibraryWork): List<CopyGroup> {
    val order = mutableListOf<String>()
    val groups = mutableMapOf<String, MutableList<LibraryCopy>>()
    work.copies.forEach { copy ->
        val key = "${copy.branch.id}|${copy.callNumber}|${copy.area}"
        groups.getOrPut(key) {
            order.add(key)
            mutableListOf()
        }.add(copy)
    }
    return order.map { key ->
        val copies = groups.getValue(key)
        val first = copies.first()
        CopyGroup(
            branch = first.branch,
            callNumber = first.callNumber,
            area = first.area,
            available = copies.count { it.status == LibraryCopyStatus.Available },
            missing = copies.count { it.status == LibraryCopyStatus.Missing },
            loans = copies.mapNotNull { (it.status as? LibraryCopyStatus.OnLoan)?.due },
            localNotes = copies.mapNotNull { (it.status as? LibraryCopyStatus.LocalUse)?.note },
        )
    }
}

@Composable
private fun CopyGroupCard(group: CopyGroup, down: Boolean, now: Instant) {
    val volume = Regex("""v\.\s?\d+""", RegexOption.IGNORE_CASE)
        .find(group.callNumber)?.value
    // Loans split into credible (future due) and stale (past due — records
    // the library never closed).
    val futureLoans = group.loans.filter { it != null && it > now }
    val staleLoans = group.loans.size - futureLoans.size
    val tone = when {
        group.available > 0 -> MaterialTheme.melon.status.ok
        group.localNotes.isNotEmpty() -> MaterialTheme.melon.palette.indigo
        else -> MaterialTheme.melon.status.bad
    }
    LibraryCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = group.branch.sigla,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        volume?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!group.branch.isNear) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.melon.status.warn,
                                    modifier = Modifier.size(14.dp),
                                )
                                Text(
                                    text = group.branch.campus.orEmpty(),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = MaterialTheme.melon.status.warn,
                                )
                            }
                        }
                    }
                    Text(
                        text = group.callNumber,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.29).sp,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                    Text(
                        text = listOf(group.area, group.branch.name)
                            .filter { it.isNotEmpty() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (down) "—" else group.available.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.66).sp,
                        ),
                        color = if (down) MaterialTheme.colorScheme.outline else tone,
                    )
                    if (!down) {
                        Text(
                            text = stringResource(
                                R.string.library_detail_copies_of,
                                group.total,
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 3.dp),
                        )
                    }
                }
            }
            if (!down) {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                )
                Column(modifier = Modifier.padding(top = 6.dp)) {
                    if (group.available > 0) {
                        CopyStatusRow(
                            icon = Icons.Filled.CheckCircle,
                            tone = MaterialTheme.melon.status.ok,
                            label = pluralStringResource(
                                R.plurals.library_detail_copy_available,
                                group.available,
                                group.available,
                            ),
                            sub = null,
                        )
                    }
                    if (futureLoans.isNotEmpty()) {
                        CopyStatusRow(
                            icon = Icons.Filled.Schedule,
                            tone = MaterialTheme.melon.status.warn,
                            label = pluralStringResource(
                                R.plurals.library_detail_copy_loaned,
                                futureLoans.size,
                                futureLoans.size,
                            ),
                            sub = futureLoans.filterNotNull().minOrNull()?.let {
                                stringResource(
                                    R.string.library_availability_next_return,
                                    formatLibraryDate(it),
                                )
                            },
                        )
                    }
                    if (staleLoans > 0) {
                        val oldest = group.loans.filterNotNull().minOrNull()
                        CopyStatusRow(
                            icon = Icons.Filled.HourglassTop,
                            tone = MaterialTheme.colorScheme.onSurfaceVariant,
                            label = pluralStringResource(
                                R.plurals.library_detail_copy_stale,
                                staleLoans,
                                staleLoans,
                            ),
                            sub = oldest?.let {
                                stringResource(
                                    R.string.library_detail_copy_stale_sub,
                                    formatLibraryYear(it),
                                )
                            },
                            faded = true,
                        )
                    }
                    if (group.localNotes.isNotEmpty()) {
                        CopyStatusRow(
                            icon = Icons.Filled.Info,
                            tone = MaterialTheme.melon.palette.indigo,
                            label = pluralStringResource(
                                R.plurals.library_detail_copy_local,
                                group.localNotes.size,
                                group.localNotes.size,
                            ),
                            sub = group.localNotes.firstOrNull { it.isNotEmpty() },
                        )
                    }
                    if (group.missing > 0) {
                        CopyStatusRow(
                            icon = Icons.Filled.Report,
                            tone = MaterialTheme.colorScheme.outline,
                            label = pluralStringResource(
                                R.plurals.library_detail_copy_missing,
                                group.missing,
                                group.missing,
                            ),
                            sub = stringResource(R.string.library_detail_copy_missing_sub),
                            faded = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyStatusRow(
    icon: ImageVector,
    tone: Color,
    label: String,
    sub: String?,
    faded: Boolean = false,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (faded) 0.7f else 1f)
            .padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tone,
            modifier = Modifier
                .size(17.dp)
                .padding(top = 1.dp),
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.14).sp,
                ),
                color = tone,
            )
            sub?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    ),
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun IdChip(label: String, value: String, onTap: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onTap)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// Server sends the ABNT citation with `**bold**` runs — render them.
private fun boldMarkdown(source: String): AnnotatedString = buildAnnotatedString {
    var remaining = source
    while (true) {
        val start = remaining.indexOf("**")
        if (start < 0) {
            append(remaining)
            break
        }
        val end = remaining.indexOf("**", start + 2)
        if (end < 0) {
            append(remaining)
            break
        }
        append(remaining.substring(0, start))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(remaining.substring(start + 2, end))
        }
        remaining = remaining.substring(end + 2)
    }
}

@Composable
private fun MissingRecordState(seedTitle: String?, onBack: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 90.dp),
    ) {
        Text(
            text = seedTitle ?: stringResource(R.string.library_detail_missing_record_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.library_detail_missing_record_body),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
        TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
            Text(text = stringResource(R.string.library_back))
        }
    }
}

@Preview
@Composable
private fun LibraryWorkScreenPreview() {
    MelonTheme {
        val work = libraryPreviewWork()
        val now = Clock.System.now()
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp),
        ) {
            AnswerCard(
                work = work,
                availability = work.availability(now),
                reading = LibraryReading.Fresh(checkedAt = now),
                refreshing = false,
                now = now,
                onRefresh = {},
            )
        }
    }
}
