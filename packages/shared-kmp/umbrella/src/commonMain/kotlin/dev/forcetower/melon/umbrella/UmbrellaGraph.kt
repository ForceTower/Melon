package dev.forcetower.melon.umbrella

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.common.ApplicationContext
import dev.forcetower.melon.core.common.ForegroundSignal
import dev.forcetower.melon.core.logging.CrashReporter
import dev.forcetower.melon.core.logging.LoggingConfig
import dev.forcetower.melon.core.logging.NoopCrashReporter
import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.BeginPasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.CompletePasskeyLoginUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveActiveSemesterCodeUseCase
import dev.forcetower.melon.feature.calendar.domain.usecase.ObserveCalendarEventsUseCase
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplineDetailUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentOffersUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.GetEnrollmentWindowUseCase
import dev.forcetower.melon.feature.enrollment.domain.usecase.SubmitEnrollmentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.FetchAcademicDocumentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveCurrentCredentialsUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.MarkAllMessagesAsReadUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.MarkMessageAsReadUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessagesInboxUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ToggleMessageStarUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveAttendanceTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveDisciplinesUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveGradeTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTomorrowPreviewUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveNextClassDayUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.ObserveSettingsUseCase
import dev.forcetower.melon.feature.settings.domain.usecase.UpdateSettingsUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.BackfillMirrorUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.FetchOnboardingStatusUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshSessionUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncMessagesUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncProfileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterListUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterUseCase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(AppScope::class)
interface UmbrellaGraph {
    val loginUseCase: LoginUseCase
    val beginPasskeyLoginUseCase: BeginPasskeyLoginUseCase
    val completePasskeyLoginUseCase: CompletePasskeyLoginUseCase
    val refreshSessionUseCase: RefreshSessionUseCase
    val backfillMirrorUseCase: BackfillMirrorUseCase
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase
    val sessionStore: SessionStore
    val logger: Logger

    // App-lifecycle foreground pulse. The native shell calls pulse() on resume;
    // time-derived flows (today/now class, schedule week, next class, next test)
    // merge it into their ticker so they recompute the current day/time instantly
    // instead of showing the day the app was backgrounded.
    val foregroundSignal: ForegroundSignal

    // SyncView orchestration surface (iOS). Each step calls one of these.
    val syncProfileUseCase: SyncProfileUseCase
    val syncSemesterListUseCase: SyncSemesterListUseCase
    val syncSemesterUseCase: SyncSemesterUseCase
    val syncMessagesUseCase: SyncMessagesUseCase
    val fetchOnboardingStatusUseCase: FetchOnboardingStatusUseCase
    val pingActivityUseCase: PingActivityUseCase

    // Dashboard read-side: end-of-onboarding snapshot.
    val getReadyOverviewUseCase: GetReadyOverviewUseCase

    // Overview (Hoje) reactive surfaces — one flow per UI section.
    val observeOverviewHeaderUseCase: ObserveOverviewHeaderUseCase
    val observeNowClassUseCase: ObserveNowClassUseCase
    val observeTodayTimelineUseCase: ObserveTodayTimelineUseCase
    val observeTomorrowPreviewUseCase: ObserveTomorrowPreviewUseCase
    val observeDisciplinesUseCase: ObserveDisciplinesUseCase
    val observeUnreadMessagesTileUseCase: ObserveUnreadMessagesTileUseCase
    val observeNextTestTileUseCase: ObserveNextTestTileUseCase
    val observeAttendanceTileUseCase: ObserveAttendanceTileUseCase
    val observeGradeTileUseCase: ObserveGradeTileUseCase
    val observeLastSyncUseCase: ObserveLastSyncUseCase

    // Schedule (Horário) reactive surface — one flow emitting the whole
    // Mon–Sun week of the active semester.
    val observeScheduleWeekUseCase: ObserveScheduleWeekUseCase

    // First future day with at least one scheduled class. Scans past the
    // current week so the iOS widget's "no class today" copy can name the
    // real next weekday even when there are no weekend classes.
    val observeNextClassDayUseCase: ObserveNextClassDayUseCase

    // Disciplinas (Boletim) reactive surface — one flow emitting the
    // current-semester cards, downloaded past semesters, and placeholders
    // for semesters whose payload hasn't been pulled yet.
    val observeDisciplinesListUseCase: ObserveDisciplinesListUseCase

    // Disciplinas detail — one flow emitting the full payload for a single
    // DisciplineOffer (groups, grade sections, classes timeline, attachments,
    // ementa). The native detail screen subscribes with the offerId it
    // received on navigation.
    val observeDisciplineDetailUseCase: ObserveDisciplineDetailUseCase

    // Lifetime CR — weighted mean across every completed discipline. Powers
    // both the Overview grade tile and the Me hero card so the value stays
    // consistent across the app.
    val calculateOverallScoreUseCase: CalculateOverallScoreUseCase

    // Mensagens (Recados) reactive surfaces — one flow for the grouped
    // inbox, one per-message detail flow for the reader, and suspend
    // mutations that persist readAt/starred to MessageState.
    val observeMessagesInboxUseCase: ObserveMessagesInboxUseCase
    val observeMessageDetailUseCase: ObserveMessageDetailUseCase
    val markMessageAsReadUseCase: MarkMessageAsReadUseCase
    val markAllMessagesAsReadUseCase: MarkAllMessagesAsReadUseCase
    val toggleMessageStarUseCase: ToggleMessageStarUseCase

    // Calendário (academic-calendar) reactive surfaces — events feed for the
    // agenda, plus the active-semester code that powers the eyebrow label.
    val observeCalendarEventsUseCase: ObserveCalendarEventsUseCase
    val observeActiveSemesterCodeUseCase: ObserveActiveSemesterCodeUseCase

    // Eu (Me) reactive surface — emits the hero identity, semester strip data,
    // CR/hours rollup, and the closest upcoming evaluation as one snapshot.
    val observeMeProfileUseCase: ObserveMeProfileUseCase

    // Eu (Me) document requests — Comprovante/Histórico PDFs pulled through
    // the backend, optionally carrying a solved reCAPTCHA token.
    val fetchAcademicDocumentUseCase: FetchAcademicDocumentUseCase

    // Matrícula (enrollment) — all live/uncached. `window` is the cheap hub-gate
    // + entry-screen check, `offers` is the full disciplines tree, and `submit`
    // runs the open → publish → close transaction server-side.
    val getEnrollmentWindowUseCase: GetEnrollmentWindowUseCase
    val getEnrollmentOffersUseCase: GetEnrollmentOffersUseCase
    val submitEnrollmentUseCase: SubmitEnrollmentUseCase

    // Configurações reactive surface — emits the active session's typed
    // upstream credentials so the Settings vault card can render them.
    val observeCurrentCredentialsUseCase: ObserveCurrentCredentialsUseCase

    // Configurações settings flow — emits the user's notification toggles
    // and grade-spoiler preference, hydrated from the profile mirror and
    // mutated via `updateSettingsUseCase`.
    val observeSettingsUseCase: ObserveSettingsUseCase
    val updateSettingsUseCase: UpdateSettingsUseCase

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides baseUrl: BaseUrl,
            @Provides loggingConfig: LoggingConfig,
            @Provides crashReporter: CrashReporter,
            @Provides appContext: ApplicationContext,
        ): UmbrellaGraph
    }
}

fun UmbrellaGraph(config: UmbrellaConfig): UmbrellaGraph {
    // Thread the API base URL into the logging config so ApiLogWriter knows
    // where to POST — the caller only has to set it once on UmbrellaConfig.
    val logging = config.logging.copy(apiBaseUrl = config.baseUrl)
    return createGraphFactory<UmbrellaGraph.Factory>()
        .create(
            BaseUrl(config.baseUrl),
            logging,
            config.crashReporter ?: NoopCrashReporter,
            config.appContext,
        )
}
