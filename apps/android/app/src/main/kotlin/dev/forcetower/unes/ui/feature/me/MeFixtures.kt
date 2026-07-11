package dev.forcetower.unes.ui.feature.me

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import dev.forcetower.unes.R
import dev.forcetower.unes.firebase.FeatureGates

// Catalogue of everything the screen renders that isn't user data: the
// shortcut tiles (icon + label + tone + hint) and the settings rows. Mirrors
// the dc `EuScreen` shortcut/settings defs and iOS `MeShortcut`/`MeSettingsRow`.
//
// User-facing strings live in `strings.xml` and are referenced by id here so
// the labels stay translatable even though the catalogue itself is fixed.
internal object MeFixtures {
    // Hero/progress placeholder used by previews only — the live screen hides
    // those cards until the KMP profile flow emits.
    val identity = ProfileIdentity(
        name = "Mariana Nogueira",
        firstName = "Mariana",
        course = "Engenharia de Computação",
        campusLabel = "UEFS · Módulo 5",
        enrollment = "26111463",
        username = "mariana.nogueira",
        avatarInitial = "M",
        semesterWeek = 21,
        semesterTotalWeeks = 21,
        progressPct = 100,
        cr = 6.7,
        crDelta = 0.1,
        attendancePercent = 94,
        semesterOrdinal = 6,
        semesterStart = "18 fev",
        semesterEnd = "8 jul",
    )

    // Grid tiles in dc/iOS order. Enrollment, Certificate, History, and
    // Paradoxo are remote-config gated; Calendar and Countdown always show.
    private val gridLibrary = listOf(
        Shortcut(
            id = ShortcutKind.Enrollment,
            labelRes = R.string.me_shortcut_enrollment_label,
            hintRes = R.string.me_shortcut_enrollment_hint,
            tone = ShortcutTone.Teal,
            icon = Icons.Filled.EditNote,
            beta = true,
        ),
        Shortcut(
            id = ShortcutKind.Calendar,
            labelRes = R.string.me_shortcut_calendar_label,
            hintRes = R.string.me_shortcut_calendar_hint,
            tone = ShortcutTone.Coral,
            icon = Icons.Filled.CalendarMonth,
        ),
        Shortcut(
            id = ShortcutKind.Countdown,
            labelRes = R.string.me_shortcut_countdown_label,
            hintRes = R.string.me_shortcut_countdown_hint,
            tone = ShortcutTone.Magenta,
            icon = Icons.Filled.HourglassTop,
        ),
        Shortcut(
            id = ShortcutKind.Certificate,
            labelRes = R.string.me_shortcut_certificate_label,
            hintRes = R.string.me_shortcut_certificate_hint,
            tone = ShortcutTone.Indigo,
            icon = Icons.Filled.Description,
        ),
        Shortcut(
            id = ShortcutKind.History,
            labelRes = R.string.me_shortcut_history_label,
            hintRes = R.string.me_shortcut_history_hint,
            tone = ShortcutTone.Violet,
            icon = Icons.AutoMirrored.Filled.ReceiptLong,
        ),
        Shortcut(
            id = ShortcutKind.Paradoxo,
            labelRes = R.string.me_shortcut_paradoxo_label,
            hintRes = R.string.me_shortcut_paradoxo_hint,
            tone = ShortcutTone.Amber,
            icon = Icons.Filled.Insights,
        ),
    )

    // Materiais renders as the wide card under the grid.
    val materials = Shortcut(
        id = ShortcutKind.Materials,
        labelRes = R.string.me_shortcut_materials_label,
        hintRes = R.string.me_shortcut_materials_hint,
        tone = ShortcutTone.Green,
        icon = Icons.AutoMirrored.Filled.MenuBook,
    )

    fun gridShortcuts(gates: FeatureGates): List<Shortcut> = gridLibrary.filter {
        when (it.id) {
            ShortcutKind.Enrollment -> gates.enrollment
            ShortcutKind.Certificate -> gates.enrollmentCertificate
            ShortcutKind.History -> gates.academicHistory
            ShortcutKind.Paradoxo -> gates.paradoxo
            else -> true
        }
    }

    val settingsRows: List<SettingsRow> = listOf(
        SettingsRow(
            id = SettingsRowKind.Settings,
            labelRes = R.string.me_settings_row_settings_label,
            hintRes = R.string.me_settings_row_settings_hint,
            icon = Icons.Filled.Settings,
            tone = null,
        ),
        SettingsRow(
            id = SettingsRowKind.About,
            labelRes = R.string.me_settings_row_about_label,
            hintRes = R.string.me_settings_row_about_hint,
            icon = Icons.Filled.Info,
            tone = ShortcutTone.Indigo,
        ),
        SettingsRow(
            id = SettingsRowKind.Feedback,
            labelRes = R.string.me_settings_row_feedback_label,
            hintRes = R.string.me_settings_row_feedback_hint,
            icon = Icons.Filled.BugReport,
            tone = ShortcutTone.Amber,
        ),
        SettingsRow(
            id = SettingsRowKind.Licenses,
            labelRes = R.string.me_settings_row_licenses_label,
            hintRes = R.string.me_settings_row_licenses_hint,
            icon = Icons.Filled.Copyright,
            tone = ShortcutTone.Violet,
        ),
    )
}
