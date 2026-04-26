package dev.forcetower.unes.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.Mesh
import dev.forcetower.unes.designsystem.foundation.MeshVariant
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.ui.feature.settings.components.CredentialCard
import dev.forcetower.unes.ui.feature.settings.components.NotificationGroupCard
import dev.forcetower.unes.ui.feature.settings.components.NotificationPreview
import dev.forcetower.unes.ui.feature.settings.components.NotificationToggleRow
import dev.forcetower.unes.ui.feature.settings.components.SettingsFooter
import dev.forcetower.unes.ui.feature.settings.components.SettingsGlyph
import dev.forcetower.unes.ui.feature.settings.components.SettingsHeader
import dev.forcetower.unes.ui.feature.settings.components.SettingsSectionHeader
import dev.forcetower.unes.ui.feature.settings.components.SpoilerPickerRow
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo

// "Configurações" — editorial settings hub. Three stacked sections: account
// (credential vault), display (grade spoiler + lock-screen preview), and
// notifications (three grouped cards with per-row toggles). Sync cadence,
// wifi gates, and frequency are server-side decisions in this rewrite, so the
// screen only exposes what the client still controls. Mirrors `SettingsView`
// on iOS and `screens-settings.jsx` in the prototype.
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val appInfo = rememberAppInfo()
    val surface = MaterialTheme.colorScheme.surface

    val syncStrings = rememberSyncStrings()
    val lastSyncLabel = remember(state.lastSyncIso, state.nowEpochSeconds, syncStrings) {
        formatLastSync(state.lastSyncIso, state.nowEpochSeconds, syncStrings)
    }

    // Reveal stays UI-only — every revisit re-mints the masked password so a
    // backgrounded app never surfaces the credentials without an explicit tap.
    var revealed by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surface),
    ) {
        // Warm mesh wash pinned behind the header, fading into the surface.
        // Same treatment as `MeScreen`/`FinalCountdownView` so the editorial
        // type reads cleanly below.
        Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
            Mesh(variant = MeshVariant.Warm, intensity = 0.5f, modifier = Modifier.fillMaxSize())
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .verticalScroll(rememberScrollState())
                .padding(bottom = bottomInset),
        ) {
            Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))

            SettingsHeader(
                onBack = onBack,
                lastSyncLabel = lastSyncLabel,
                appVersion = appInfo.version,
                modifier = Modifier.fadeUpOnAppear(delayMs = 20),
            )

            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
            ) {
                // capítulo 1 · Conta
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 120)) {
                    SettingsSectionHeader(
                        eyebrow = stringResource(R.string.settings_section_account_eyebrow),
                        title = stringResource(R.string.settings_section_account_title),
                        meta = stringResource(R.string.settings_section_account_meta),
                    )
                    CredentialCard(
                        username = state.username.orEmpty(),
                        password = state.password.orEmpty(),
                        revealed = revealed,
                        onToggleReveal = { revealed = !revealed },
                    )
                }

                // capítulo 2 · Exibição
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 220)) {
                    SettingsSectionHeader(
                        eyebrow = stringResource(R.string.settings_section_display_eyebrow),
                        title = stringResource(R.string.settings_section_display_title),
                        meta = stringResource(R.string.settings_section_display_meta),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        NotificationPreview(spoiler = state.spoiler)
                        SpoilerPickerRow(
                            value = state.spoiler,
                            onChange = { vm.onIntent(SettingsIntent.SetSpoiler(it)) },
                        )
                    }
                }

                // capítulo 3 · Notificações
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 320)) {
                    SettingsSectionHeader(
                        eyebrow = stringResource(R.string.settings_section_notifications_eyebrow),
                        title = stringResource(R.string.settings_section_notifications_title),
                        meta = stringResource(
                            R.string.settings_section_notifications_meta_format,
                            state.totalActive,
                        ),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        MessagesGroup(state = state, onToggle = vm::onIntent)
                        GradesGroup(state = state, onToggle = vm::onIntent)
                        ClassesGroup(state = state, onToggle = vm::onIntent)
                    }
                }

                SettingsFooter(
                    appVersion = appInfo.version,
                    appBuild = appInfo.build,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 440),
                )
            }
        }
    }
}

@Composable
private fun MessagesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        kicker = stringResource(R.string.settings_notif_group_messages_kicker),
        title = stringResource(R.string.settings_notif_group_messages_title),
        activeCount = state.messageActiveCount,
        rows = {
            NotificationToggleRow(
                glyph = SettingsGlyph.Megaphone,
                tone = SettingsTone.Amber,
                label = stringResource(R.string.settings_notif_msg_broadcast_label),
                hint = stringResource(R.string.settings_notif_msg_broadcast_hint),
                on = state.notifMsgBroadcast,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgBroadcast, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Users,
                tone = SettingsTone.Teal,
                label = stringResource(R.string.settings_notif_msg_class_label),
                hint = stringResource(R.string.settings_notif_msg_class_hint),
                on = state.notifMsgClass,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgClass, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Envelope,
                tone = SettingsTone.Plum,
                label = stringResource(R.string.settings_notif_msg_direct_label),
                hint = stringResource(R.string.settings_notif_msg_direct_hint),
                on = state.notifMsgDirect,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgDirect, it)) },
                showSeparator = false,
            )
        },
    )
}

@Composable
private fun GradesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        kicker = stringResource(R.string.settings_notif_group_grades_kicker),
        title = stringResource(R.string.settings_notif_group_grades_title),
        activeCount = state.gradeActiveCount,
        rows = {
            NotificationToggleRow(
                glyph = SettingsGlyph.Sparkle,
                tone = SettingsTone.Coral,
                label = stringResource(R.string.settings_notif_grade_posted_label),
                hint = stringResource(R.string.settings_notif_grade_posted_hint),
                on = state.notifGradePosted,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradePosted, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Pencil,
                tone = SettingsTone.Magenta,
                label = stringResource(R.string.settings_notif_grade_changed_label),
                hint = stringResource(R.string.settings_notif_grade_changed_hint),
                on = state.notifGradeChanged,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradeChanged, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Calendar,
                tone = SettingsTone.Plum,
                label = stringResource(R.string.settings_notif_grade_date_changed_label),
                hint = stringResource(R.string.settings_notif_grade_date_changed_hint),
                on = state.notifGradeDateChanged,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradeDateChanged, it)) },
                showSeparator = false,
            )
        },
    )
}

@Composable
private fun ClassesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        kicker = stringResource(R.string.settings_notif_group_classes_kicker),
        title = stringResource(R.string.settings_notif_group_classes_title),
        activeCount = state.classActiveCount,
        rows = {
            NotificationToggleRow(
                glyph = SettingsGlyph.Pin,
                tone = SettingsTone.Teal,
                label = stringResource(R.string.settings_notif_class_location_label),
                hint = stringResource(R.string.settings_notif_class_location_hint),
                on = state.notifClassLocation,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassLocation, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Book,
                tone = SettingsTone.Amber,
                label = stringResource(R.string.settings_notif_class_material_label),
                hint = stringResource(R.string.settings_notif_class_material_hint),
                on = state.notifClassMaterial,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassMaterial, it)) },
            )
            NotificationToggleRow(
                glyph = SettingsGlyph.Tag,
                tone = SettingsTone.Coral,
                label = stringResource(R.string.settings_notif_class_subject_label),
                hint = stringResource(R.string.settings_notif_class_subject_hint),
                on = state.notifClassSubject,
                onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassSubject, it)) },
                showSeparator = false,
            )
        },
    )
}

// String bag for the relative sync formatter — pulled once via
// `stringResource` so the format function below stays non-composable.
private data class SyncStrings(
    val unknown: String,
    val now: String,
    val minutesFormat: String,
    val hoursFormat: String,
    val daysFormat: String,
)

@Composable
private fun rememberSyncStrings(): SyncStrings {
    val unknown = stringResource(R.string.settings_last_sync_unknown)
    val now = stringResource(R.string.settings_last_sync_now)
    val minutes = stringResource(R.string.settings_last_sync_minutes_format)
    val hours = stringResource(R.string.settings_last_sync_hours_format)
    val days = stringResource(R.string.settings_last_sync_days_format)
    return remember(unknown, now, minutes, hours, days) {
        SyncStrings(unknown, now, minutes, hours, days)
    }
}

// "há N min" / "há N h" / "há N d" — same buckets as iOS
// `SettingsViewModel.formatRelative`.
private fun formatLastSync(iso: String?, nowEpochSeconds: Long, strings: SyncStrings): String {
    if (iso.isNullOrBlank()) return strings.unknown
    val parsed = runCatching { java.time.Instant.parse(iso).epochSecond }.getOrNull()
        ?: return strings.unknown
    val seconds = (nowEpochSeconds - parsed).coerceAtLeast(0L)
    val minutes = (seconds / 60L).toInt()
    if (minutes < 1) return strings.now
    if (minutes < 60) return strings.minutesFormat.format(minutes)
    val hours = minutes / 60
    if (hours < 24) return strings.hoursFormat.format(hours)
    return strings.daysFormat.format(hours / 24)
}
