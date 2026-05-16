package dev.forcetower.unes.ui.feature.me

import dev.forcetower.unes.R

// Fixtures for everything the screen renders that isn't user data: the
// shortcut catalogue (icon + label + tone + hint per tile) and the services
// list. Mirrors `apps/ios/UNES/Features/Me/Models/MeFixtures.swift`.
//
// User-facing strings live in `strings.xml` and are referenced by id here so
// the labels stay translatable even though the catalogue itself is fixed.
internal object MeFixtures {
    // Hero/strip placeholder used by previews and as the initial value while
    // the KMP profile flow hasn't emitted yet.
    val identity = ProfileIdentity(
        name = "Mariana Nogueira",
        firstName = "Mariana",
        course = "Engenharia de Computação",
        campus = "Universidade Estadual de Feira de Santana",
        enrollment = "26111463",
        username = "mariana.nogueira",
        avatarInitial = "M",
        semester = "2026.1",
        semesterWeek = 7,
        semesterTotalWeeks = 18,
        progressPct = 38,
        cr = 8.5,
        crDelta = "+0,3",
        creditsDone = 142,
        creditsRequired = 240,
        semesterStart = "início · 24 fev",
        semesterEnd = "fim · 17 jul",
        finalExam = "prova final · 07 jul",
    )

    val library: Map<ShortcutKind, Shortcut> = mapOf(
        ShortcutKind.Account to Shortcut(
            id = ShortcutKind.Account,
            labelRes = R.string.me_shortcut_account_label,
            hintRes = R.string.me_shortcut_account_hint,
            tone = ShortcutTone.Plum,
            icon = ShortcutIcon.Account,
        ),
        ShortcutKind.Zhonya to Shortcut(
            id = ShortcutKind.Zhonya,
            labelRes = R.string.me_shortcut_zhonya_label,
            hintRes = R.string.me_shortcut_zhonya_hint,
            tone = ShortcutTone.Magenta,
            icon = ShortcutIcon.Hourglass,
        ),
        ShortcutKind.Flowchart to Shortcut(
            id = ShortcutKind.Flowchart,
            labelRes = R.string.me_shortcut_flowchart_label,
            hintRes = R.string.me_shortcut_flowchart_hint,
            tone = ShortcutTone.Teal,
            icon = ShortcutIcon.Flow,
        ),
        ShortcutKind.Bandejao to Shortcut(
            id = ShortcutKind.Bandejao,
            labelRes = R.string.me_shortcut_bandejao_label,
            hintRes = R.string.me_shortcut_bandejao_hint,
            tone = ShortcutTone.Amber,
            icon = ShortcutIcon.Tray,
        ),
        ShortcutKind.Calendar to Shortcut(
            id = ShortcutKind.Calendar,
            labelRes = R.string.me_shortcut_calendar_label,
            hintRes = R.string.me_shortcut_calendar_hint,
            tone = ShortcutTone.Coral,
            icon = ShortcutIcon.Calendar,
        ),
        ShortcutKind.Countdown to Shortcut(
            id = ShortcutKind.Countdown,
            labelRes = R.string.me_shortcut_countdown_label,
            hintRes = R.string.me_shortcut_countdown_hint,
            tone = ShortcutTone.Plum,
            icon = ShortcutIcon.Timer,
        ),
        ShortcutKind.Request to Shortcut(
            id = ShortcutKind.Request,
            labelRes = R.string.me_shortcut_request_label,
            hintRes = R.string.me_shortcut_request_hint,
            tone = ShortcutTone.Teal,
            icon = ShortcutIcon.Doc,
        ),
        ShortcutKind.Theme to Shortcut(
            id = ShortcutKind.Theme,
            labelRes = R.string.me_shortcut_theme_label,
            hintRes = R.string.me_shortcut_theme_hint,
            tone = ShortcutTone.Magenta,
            icon = ShortcutIcon.Brush,
        ),
        ShortcutKind.Reminders to Shortcut(
            id = ShortcutKind.Reminders,
            labelRes = R.string.me_shortcut_reminders_label,
            hintRes = R.string.me_shortcut_reminders_hint,
            tone = ShortcutTone.Coral,
            icon = ShortcutIcon.Bell,
        ),
        ShortcutKind.Adventure to Shortcut(
            id = ShortcutKind.Adventure,
            labelRes = R.string.me_shortcut_adventure_label,
            hintRes = R.string.me_shortcut_adventure_hint,
            tone = ShortcutTone.Amber,
            icon = ShortcutIcon.Compass,
        ),
    )

    val defaultPinned: List<ShortcutKind> = listOf(
        ShortcutKind.Calendar,
        ShortcutKind.Countdown,
    )

    fun pinned(ids: List<ShortcutKind>): List<Shortcut> =
        ids.mapNotNull { library[it] }

    val settingsRows: List<SettingsRow> = listOf(
        SettingsRow(
            id = SettingsRowKind.Settings,
            labelRes = R.string.me_settings_row_settings_label,
            hintRes = R.string.me_settings_row_settings_hint,
            icon = SettingsIcon.Gear,
        ),
        SettingsRow(
            id = SettingsRowKind.About,
            labelRes = R.string.me_settings_row_about_label,
            hintRes = R.string.me_settings_row_about_hint,
            icon = SettingsIcon.Info,
        ),
        SettingsRow(
            id = SettingsRowKind.Feedback,
            labelRes = R.string.me_settings_row_feedback_label,
            hintRes = R.string.me_settings_row_feedback_hint,
            icon = SettingsIcon.Bug,
        ),
        SettingsRow(
            id = SettingsRowKind.Licenses,
            labelRes = R.string.me_settings_row_licenses_label,
            hintRes = R.string.me_settings_row_licenses_hint,
            icon = SettingsIcon.License,
        ),
    )
}
