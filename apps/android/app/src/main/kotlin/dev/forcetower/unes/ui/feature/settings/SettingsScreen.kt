package dev.forcetower.unes.ui.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.forcetower.unes.R
import dev.forcetower.unes.designsystem.foundation.PinnedHeaderHairline
import dev.forcetower.unes.designsystem.foundation.fadeUpOnAppear
import dev.forcetower.unes.designsystem.theme.melon
import dev.forcetower.unes.theme.ThemeMode
import dev.forcetower.unes.ui.feature.me.components.rememberAppInfo
import dev.forcetower.unes.ui.feature.settings.components.NotificationGroupCard
import dev.forcetower.unes.ui.feature.settings.components.NotificationPreview
import dev.forcetower.unes.ui.feature.settings.components.NotificationToggleRow
import dev.forcetower.unes.ui.feature.settings.components.SettingsFooter
import dev.forcetower.unes.ui.feature.settings.components.SettingsOptionCard
import dev.forcetower.unes.ui.feature.settings.components.SettingsSectionLabel
import dev.forcetower.unes.ui.feature.settings.components.SettingsSegmentedRow
import dev.forcetower.unes.ui.feature.settings.components.VaultCard
import java.util.Date

// "Configurações" — dc `UNES Configurações - Android`. M3 large-app-bar
// header, the always-dark credentials vault, Tema + Privacidade segmented
// cards (the notification preview reacts to the privacy choice), and the
// three notification groups with native switches. Sync cadence, wifi gates,
// and frequency stay server-side decisions, so the screen only exposes what
// the client still controls.
@Composable
internal fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPasskeys: () -> Unit,
    modifier: Modifier = Modifier,
    bottomInset: Dp = 0.dp,
) {
    val vm: SettingsViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val appInfo = rememberAppInfo()
    val context = LocalContext.current

    // The passkey count lives behind a live GET, so refresh it whenever the
    // screen (re)appears — e.g. after the manager screen adds or revokes a key.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refreshPasskeys() }

    // Respects the system 12/24-hour setting, like a real lock screen would.
    val clockLabel = remember(state.nowEpochSeconds) {
        if (state.nowEpochSeconds == 0L) ""
        else android.text.format.DateFormat.getTimeFormat(context)
            .format(Date(state.nowEpochSeconds * 1000L))
    }

    // Reveal stays UI-only — every revisit re-mints the masked password so a
    // backgrounded app never surfaces the credentials without an explicit tap.
    var revealed by rememberSaveable { mutableStateOf(false) }

    val scrollState = rememberScrollState()
    val scrolled by remember { derivedStateOf { scrollState.value > 0 } }

    // The back-button bar stays pinned; the headline and the settings cards
    // scroll beneath it.
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        SettingsTopBar(onBack = onBack, modifier = Modifier.fadeUpOnAppear(delayMs = 20))
        PinnedHeaderHairline(scrolled = scrolled)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = bottomInset + 16.dp),
        ) {
            SettingsHeadline(modifier = Modifier.fadeUpOnAppear(delayMs = 40))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                VaultCard(
                    displayName = state.displayName,
                    accountLabel = stringResource(
                        R.string.settings_vault_account_format,
                        state.campusLabel
                            ?: stringResource(R.string.settings_vault_account_fallback),
                    ),
                    avatarInitial = state.avatarInitial,
                    username = state.username.orEmpty(),
                    password = state.password.orEmpty(),
                    revealed = revealed,
                    onToggleReveal = {
                        if (!revealed) vm.trackCredentialReveal()
                        revealed = !revealed
                    },
                    passkeyCount = state.passkeyCount,
                    onOpenPasskeys = onOpenPasskeys,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 120),
                )

                // ── Aparência ──
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 200)) {
                    Spacer(Modifier.height(28.dp))
                    SettingsSectionLabel(
                        label = stringResource(R.string.settings_section_appearance),
                    )
                    Spacer(Modifier.height(12.dp))
                    SettingsOptionCard(
                        icon = Icons.Filled.Palette,
                        iconTint = MaterialTheme.melon.status.warn,
                        title = stringResource(R.string.settings_theme_title),
                        subtitle = stringResource(R.string.settings_theme_subtitle),
                    ) {
                        SettingsSegmentedRow(
                            options = ThemeModeOptions,
                            selected = state.themeMode,
                            optionLabel = { stringResource(it.labelRes()) },
                            onSelect = { vm.onIntent(SettingsIntent.SetTheme(it)) },
                        )
                    }
                }

                // ── Notas · privacidade ──
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 260)) {
                    Spacer(Modifier.height(28.dp))
                    SettingsSectionLabel(
                        label = stringResource(R.string.settings_section_grades),
                        meta = stringResource(R.string.settings_section_grades_meta),
                    )
                    Spacer(Modifier.height(12.dp))
                    NotificationPreview(spoiler = state.spoiler, clockLabel = clockLabel)
                    Spacer(Modifier.height(12.dp))
                    SettingsOptionCard(
                        icon = Icons.Filled.Shield,
                        iconTint = MaterialTheme.melon.palette.coral,
                        title = stringResource(R.string.settings_privacy_title),
                        subtitle = stringResource(R.string.settings_privacy_subtitle),
                    ) {
                        SettingsSegmentedRow(
                            options = SpoilerMode.entries,
                            selected = state.spoiler,
                            optionLabel = { stringResource(it.labelRes()) },
                            onSelect = { vm.onIntent(SettingsIntent.SetSpoiler(it)) },
                        )
                    }
                }

                // ── Notificações ──
                Column(modifier = Modifier.fadeUpOnAppear(delayMs = 320)) {
                    Spacer(Modifier.height(28.dp))
                    SettingsSectionLabel(
                        label = stringResource(R.string.settings_section_notifications),
                        meta = stringResource(
                            R.string.settings_notif_total_format,
                            state.totalActive,
                        ),
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        MessagesGroup(state = state, onToggle = vm::onIntent)
                        GradesGroup(state = state, onToggle = vm::onIntent)
                        ClassesGroup(state = state, onToggle = vm::onIntent)
                        if (state.evaluationRemindersAvailable) {
                            EvaluationReminderCard(state = state, onToggle = vm::onIntent)
                        }
                    }
                }

                SettingsFooter(
                    appVersion = appInfo.version,
                    appBuild = appInfo.build,
                    modifier = Modifier.fadeUpOnAppear(delayMs = 400),
                )
            }
        }
    }
}

// Pinned back-affordance bar; the display title + mission statement live in
// `SettingsHeadline`, which scrolls with the content.
@Composable
private fun SettingsTopBar(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 4.dp),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .offset(x = (-10).dp)
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.settings_back),
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun SettingsHeadline(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 6.dp, bottom = 20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.displaySmall.copy(
                fontSize = 32.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.64).sp,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                lineHeight = 20.sp,
            ),
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun MessagesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        title = stringResource(R.string.settings_notif_group_messages_title),
        activeCount = state.messageActiveCount,
        totalCount = 3,
    ) {
        NotificationToggleRow(
            icon = Icons.Filled.Campaign,
            iconTint = MaterialTheme.melon.palette.coral,
            label = stringResource(R.string.settings_notif_msg_broadcast_label),
            hint = stringResource(R.string.settings_notif_msg_broadcast_hint),
            on = state.notifMsgBroadcast,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgBroadcast, it)) },
            showDivider = false,
        )
        NotificationToggleRow(
            icon = Icons.Filled.Groups,
            iconTint = MaterialTheme.melon.palette.jade,
            label = stringResource(R.string.settings_notif_msg_class_label),
            hint = stringResource(R.string.settings_notif_msg_class_hint),
            on = state.notifMsgClass,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgClass, it)) },
        )
        NotificationToggleRow(
            icon = Icons.Filled.Mail,
            iconTint = MaterialTheme.melon.palette.violet,
            label = stringResource(R.string.settings_notif_msg_direct_label),
            hint = stringResource(R.string.settings_notif_msg_direct_hint),
            on = state.notifMsgDirect,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.MsgDirect, it)) },
        )
    }
}

@Composable
private fun GradesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        title = stringResource(R.string.settings_notif_group_grades_title),
        activeCount = state.gradeActiveCount,
        totalCount = 3,
    ) {
        NotificationToggleRow(
            icon = Icons.Filled.AutoAwesome,
            iconTint = MaterialTheme.melon.palette.coral,
            label = stringResource(R.string.settings_notif_grade_posted_label),
            hint = stringResource(R.string.settings_notif_grade_posted_hint),
            on = state.notifGradePosted,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradePosted, it)) },
            showDivider = false,
        )
        NotificationToggleRow(
            icon = Icons.Filled.Edit,
            iconTint = MaterialTheme.melon.palette.magenta,
            label = stringResource(R.string.settings_notif_grade_changed_label),
            hint = stringResource(R.string.settings_notif_grade_changed_hint),
            on = state.notifGradeChanged,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradeChanged, it)) },
        )
        NotificationToggleRow(
            icon = Icons.Filled.Event,
            iconTint = MaterialTheme.melon.palette.violet,
            label = stringResource(R.string.settings_notif_grade_date_changed_label),
            hint = stringResource(R.string.settings_notif_grade_date_changed_hint),
            on = state.notifGradeDateChanged,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.GradeDateChanged, it)) },
        )
    }
}

@Composable
private fun ClassesGroup(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    NotificationGroupCard(
        title = stringResource(R.string.settings_notif_group_classes_title),
        activeCount = state.classActiveCount,
        totalCount = 3,
    ) {
        NotificationToggleRow(
            icon = Icons.Filled.LocationOn,
            iconTint = MaterialTheme.melon.palette.jade,
            label = stringResource(R.string.settings_notif_class_location_label),
            hint = stringResource(R.string.settings_notif_class_location_hint),
            on = state.notifClassLocation,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassLocation, it)) },
            showDivider = false,
        )
        NotificationToggleRow(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            iconTint = MaterialTheme.melon.status.warn,
            label = stringResource(R.string.settings_notif_class_material_label),
            hint = stringResource(R.string.settings_notif_class_material_hint),
            on = state.notifClassMaterial,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassMaterial, it)) },
        )
        NotificationToggleRow(
            icon = Icons.Filled.Sell,
            iconTint = MaterialTheme.melon.palette.coral,
            label = stringResource(R.string.settings_notif_class_subject_label),
            hint = stringResource(R.string.settings_notif_class_subject_hint),
            on = state.notifClassSubject,
            onToggle = { onToggle(SettingsIntent.SetToggle(NotifToggle.ClassSubject, it)) },
        )
    }
}

// Device-local evening-before reminder — a single-row card without the
// server groups' header counter, because it schedules on this device and
// never PATCHes `user_settings`.
@Composable
private fun EvaluationReminderCard(state: SettingsUiState, onToggle: (SettingsIntent) -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.melon.surface.card)
            .border(1.dp, MaterialTheme.melon.surface.line, shape),
    ) {
        NotificationToggleRow(
            icon = Icons.Filled.Alarm,
            iconTint = MaterialTheme.melon.palette.violet,
            label = stringResource(R.string.settings_notif_evaluation_reminder_label),
            hint = stringResource(R.string.settings_notif_evaluation_reminder_hint),
            on = state.evaluationRemindersEnabled,
            onToggle = { onToggle(SettingsIntent.SetEvaluationReminders(it)) },
            showDivider = false,
        )
    }
}

// Claro / Sistema / Escuro, in dc order.
private val ThemeModeOptions = listOf(ThemeMode.Light, ThemeMode.System, ThemeMode.Dark)

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.Light -> R.string.settings_theme_light
    ThemeMode.System -> R.string.settings_theme_system
    ThemeMode.Dark -> R.string.settings_theme_dark
}

private fun SpoilerMode.labelRes(): Int = when (this) {
    SpoilerMode.Value -> R.string.settings_privacy_value
    SpoilerMode.Comment -> R.string.settings_privacy_summary
    SpoilerMode.Posted -> R.string.settings_privacy_discreet
}
