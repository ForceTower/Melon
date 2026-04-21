package dev.forcetower.melon.umbrella

import dev.forcetower.melon.core.network.BaseUrl
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.auth.domain.usecase.LoginUseCase
import dev.forcetower.melon.feature.dashboard.domain.usecase.GetReadyOverviewUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.notifications.domain.usecase.RegisterNotificationTokenUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveAttendanceTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveDisciplinesUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveLastSyncUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNextTestTileUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveNowClassUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveOverviewHeaderUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveTodayTimelineUseCase
import dev.forcetower.melon.feature.overview.domain.usecase.ObserveUnreadMessagesTileUseCase
import dev.forcetower.melon.feature.schedule.domain.usecase.ObserveScheduleWeekUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.FetchOnboardingStatusUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.PingActivityUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.RefreshActiveSemestersUseCase
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
    val refreshUseCase: RefreshActiveSemestersUseCase
    val registerNotificationTokenUseCase: RegisterNotificationTokenUseCase
    val sessionStore: SessionStore

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
    val observeLastSyncUseCase: ObserveLastSyncUseCase

    // Schedule (Horário) reactive surface — one flow emitting the whole
    // Mon–Sun week of the active semester.
    val observeScheduleWeekUseCase: ObserveScheduleWeekUseCase

    // Disciplinas (Boletim) reactive surface — one flow emitting the
    // current-semester cards, downloaded past semesters, and placeholders
    // for semesters whose payload hasn't been pulled yet.
    val observeDisciplinesListUseCase: ObserveDisciplinesListUseCase

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides baseUrl: BaseUrl): UmbrellaGraph
    }
}

fun UmbrellaGraph(config: UmbrellaConfig): UmbrellaGraph =
    createGraphFactory<UmbrellaGraph.Factory>().create(BaseUrl(config.baseUrl))
