package dev.forcetower.melon.umbrella

import co.touchlab.kermit.Logger
import dev.forcetower.melon.core.logging.LoggingConfig
import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplineDetailUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.MarkMessageAsReadUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessageDetailUseCase
import dev.forcetower.melon.feature.messages.domain.usecase.ObserveMessagesInboxUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveAttendanceTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveDisciplinesUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveGradeTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
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
    val refreshSessionUseCase: RefreshSessionUseCase
    val backfillMirrorUseCase: BackfillMirrorUseCase
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase
    val sessionStore: SessionStore
    val logger: Logger

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
    val observeDisciplinesUseCase: ObserveDisciplinesUseCase
    val observeUnreadMessagesTileUseCase: ObserveUnreadMessagesTileUseCase
    val observeNextTestTileUseCase: ObserveNextTestTileUseCase
    val observeAttendanceTileUseCase: ObserveAttendanceTileUseCase
    val observeGradeTileUseCase: ObserveGradeTileUseCase
    val observeLastSyncUseCase: ObserveLastSyncUseCase

    // Schedule (Horário) reactive surface — one flow emitting the whole
    // Mon–Sun week of the active semester.
    val observeScheduleWeekUseCase: ObserveScheduleWeekUseCase

    // Disciplinas (Boletim) reactive surface — one flow emitting the
    // current-semester cards, downloaded past semesters, and placeholders
    // for semesters whose payload hasn't been pulled yet.
    val observeDisciplinesListUseCase: ObserveDisciplinesListUseCase

    // Disciplinas detail — one flow emitting the full payload for a single
    // DisciplineOffer (groups, grade sections, classes timeline, attachments,
    // ementa). The native detail screen subscribes with the offerId it
    // received on navigation.
    val observeDisciplineDetailUseCase: ObserveDisciplineDetailUseCase

    // Mensagens (Recados) reactive surfaces — one flow for the grouped
    // inbox, one per-message detail flow for the reader, and a suspend
    // mutation that persists readAt to MessageState on first view.
    val observeMessagesInboxUseCase: ObserveMessagesInboxUseCase
    val observeMessageDetailUseCase: ObserveMessageDetailUseCase
    val markMessageAsReadUseCase: MarkMessageAsReadUseCase

    // Eu (Me) reactive surface — emits the hero identity, semester strip data,
    // CR/hours rollup, and the closest upcoming evaluation as one snapshot.
    val observeMeProfileUseCase: ObserveMeProfileUseCase

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides baseUrl: BaseUrl,
            @Provides loggingConfig: LoggingConfig,
        ): UmbrellaGraph
    }
}

fun UmbrellaGraph(config: UmbrellaConfig): UmbrellaGraph =
    createGraphFactory<UmbrellaGraph.Factory>()
        .create(BaseUrl(config.baseUrl), config.logging)
