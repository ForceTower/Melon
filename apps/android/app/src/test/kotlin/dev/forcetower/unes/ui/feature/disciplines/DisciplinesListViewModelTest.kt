package dev.forcetower.unes.ui.feature.disciplines

import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.sync.domain.model.SyncError
import dev.forcetower.melon.feature.disciplines.domain.model.DisciplinesListState
import dev.forcetower.melon.feature.disciplines.domain.model.PendingSemester
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.disciplines.domain.usecase.ObserveDisciplinesListUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncSemesterUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class DisciplinesListViewModelTest {
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun failedDownloadReportsTheSemesterAndRetryCanSucceed() =
        runTest {
            val syncSemester = mockk<SyncSemesterUseCase>()
            coEvery { syncSemester("s1") } returnsMany listOf(Outcome.Err(SyncError.NoConnection), Outcome.Ok(Unit))
            val model = model(syncSemester)
            val effects = mutableListOf<DisciplinesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }
            advanceUntilIdle()

            model.onIntent(DisciplinesIntent.Download("2026.1"))
            advanceUntilIdle()
            assertEquals(listOf<DisciplinesEffect>(DisciplinesEffect.DownloadFailed("2026.1")), effects)
            assertTrue(model.state.value.downloading.isEmpty())

            model.onIntent(DisciplinesIntent.Download("2026.1"))
            advanceUntilIdle()
            assertEquals(1, effects.size)
            coVerify(exactly = 2) { syncSemester("s1") }
        }

    @Test
    fun aThrowingDownloadIsReportedLikeAFailedOne() =
        runTest {
            val syncSemester = mockk<SyncSemesterUseCase>()
            coEvery { syncSemester("s1") } throws IllegalStateException("synthetic")
            val model = model(syncSemester)
            val effects = mutableListOf<DisciplinesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.effects.toList(effects) }
            advanceUntilIdle()

            model.onIntent(DisciplinesIntent.Download("2026.1"))
            advanceUntilIdle()

            assertEquals(listOf<DisciplinesEffect>(DisciplinesEffect.DownloadFailed("2026.1")), effects)
            assertTrue(model.state.value.downloading.isEmpty())
        }

    private fun model(syncSemester: SyncSemesterUseCase): DisciplinesListViewModel {
        val observeList = mockk<ObserveDisciplinesListUseCase>()
        val calculateOverallScore = mockk<CalculateOverallScoreUseCase>()
        every { observeList() } returns flowOf(
            DisciplinesListState(
                current = null,
                past = emptyList(),
                pending = listOf(PendingSemester(semesterId = "s1", semesterCode = "2026.1", track = null)),
            ),
        )
        every { calculateOverallScore() } returns emptyFlow()
        return DisciplinesListViewModel(
            observeList = observeList,
            calculateOverallScore = calculateOverallScore,
            syncSemester = syncSemester,
            analytics = NoOpAnalytics,
        )
    }
}
