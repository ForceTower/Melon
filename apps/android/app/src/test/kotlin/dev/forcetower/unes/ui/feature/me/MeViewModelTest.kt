package dev.forcetower.unes.ui.feature.me

import dev.forcetower.melon.core.analytics.Analytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.core.session.domain.SessionStore
import dev.forcetower.melon.feature.campusevent.domain.usecase.ClearCampusEventUseCase
import dev.forcetower.melon.feature.disciplines.domain.model.OverallScore
import dev.forcetower.melon.feature.disciplines.domain.usecase.CalculateOverallScoreUseCase
import dev.forcetower.melon.feature.me.domain.model.MeEnrollmentSummary
import dev.forcetower.melon.feature.me.domain.model.MeIdentity
import dev.forcetower.melon.feature.me.domain.model.MeProfile
import dev.forcetower.melon.feature.me.domain.model.ProfileUpdateError
import dev.forcetower.melon.feature.me.domain.usecase.FetchAcademicDocumentUseCase
import dev.forcetower.melon.feature.me.domain.usecase.ObserveMeProfileUseCase
import dev.forcetower.melon.feature.me.domain.usecase.UpdateProfileUseCase
import dev.forcetower.melon.feature.sync.domain.usecase.SyncProfileUseCase
import dev.forcetower.melon.feature.transit.domain.usecase.ClearCommutePlanUseCase
import dev.forcetower.unes.commute.CommuteLiveUpdates
import dev.forcetower.unes.firebase.PushRegistrar
import dev.forcetower.unes.remote.FeatureFlags
import dev.forcetower.unes.remote.FeatureGates
import dev.forcetower.unes.ui.feature.me.documents.LocalDocumentStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
internal class MeViewModelTest {
    private val sessionStore = mockk<SessionStore>(relaxed = true)
    private val pushRegistrar = mockk<PushRegistrar>().also { registrar ->
        coEvery { registrar.unregisterAll(any()) } coAnswers { firstArg<suspend () -> Unit>().invoke() }
    }
    private val updateProfile = mockk<UpdateProfileUseCase>()

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
    fun aTapThroughTheLogoutFlashDoesNotReopenTheConfirmation() =
        runTest {
            val model = model()
            advanceUntilIdle()

            model.onIntent(MeIntent.BeginLogout)
            model.onIntent(MeIntent.ConfirmLogout)
            model.onIntent(MeIntent.BeginLogout)
            runCurrent()

            assertEquals(LogoutStep.Flashing, model.state.value.logoutStep)
        }

    @Test
    fun confirmingTwiceTearsTheSessionDownOnce() =
        runTest {
            val model = model()
            advanceUntilIdle()

            model.onIntent(MeIntent.BeginLogout)
            model.onIntent(MeIntent.ConfirmLogout)
            model.onIntent(MeIntent.ConfirmLogout)
            advanceUntilIdle()

            coVerify(exactly = 1) { sessionStore.logout() }
            assertEquals(LogoutStep.LoggedOut, model.state.value.logoutStep)
            assertEquals("Ana", model.state.value.logoutFirstName)
        }

    @Test
    fun theConfirmationWaitsForTheProfileItNames() =
        runTest {
            val model = model(profileFlow = emptyFlow())
            advanceUntilIdle()

            model.onIntent(MeIntent.BeginLogout)

            assertEquals(LogoutStep.Idle, model.state.value.logoutStep)
        }

    @Test
    fun theEditSheetStaysOpenUntilItsSaveLands() =
        runTest {
            val saved = CompletableDeferred<Outcome<Unit, ProfileUpdateError>>()
            coEvery { updateProfile.updateName(any()) } coAnswers { saved.await() }
            val model = model()
            advanceUntilIdle()

            model.onIntent(MeIntent.OpenEditProfile)
            model.onIntent(MeIntent.EditNameChanged("Aninha"))
            model.onIntent(MeIntent.SaveProfile)
            runCurrent()
            model.onIntent(MeIntent.CloseEditProfile)
            model.onIntent(MeIntent.OpenEditProfile)
            model.onIntent(MeIntent.EditNameChanged("Outra"))
            model.onIntent(MeIntent.SaveProfile)
            runCurrent()

            val sheet = assertNotNull(model.state.value.editProfile)
            assertTrue(sheet.saving)
            coVerify(exactly = 1) { updateProfile.updateName(any()) }

            saved.complete(Outcome.Ok(Unit))
            runCurrent()

            coVerify(exactly = 1) { updateProfile.updateName("Aninha") }
            assertNull(model.state.value.editProfile)
            assertEquals(MeProfileToast.NameSaved("Aninha"), model.state.value.profileToast)
        }

    private fun model(profileFlow: Flow<MeProfile> = flowOf(profile)): MeViewModel {
        val observeProfile = mockk<ObserveMeProfileUseCase>()
        every { observeProfile() } returns profileFlow
        val overallScore = mockk<CalculateOverallScoreUseCase>()
        every { overallScore() } returns flowOf(OverallScore.Empty)
        val featureFlags = mockk<FeatureFlags>()
        every { featureFlags.gates } returns MutableStateFlow(FeatureGates())
        return MeViewModel(
            observeMeProfile = observeProfile,
            overallScore = overallScore,
            featureFlags = featureFlags,
            fetchDocument = mockk<FetchAcademicDocumentUseCase>(),
            updateProfile = updateProfile,
            syncProfile = mockk<SyncProfileUseCase>(relaxed = true),
            localDocuments = mockk<LocalDocumentStore>(),
            sessionStore = sessionStore,
            clearCampusEvent = mockk<ClearCampusEventUseCase>(relaxed = true),
            clearCommutePlan = mockk<ClearCommutePlanUseCase>(relaxed = true),
            pushRegistrar = pushRegistrar,
            commuteLiveUpdates = mockk<CommuteLiveUpdates>(relaxed = true),
            analytics = mockk<Analytics>(relaxed = true),
        )
    }

    private val profile = MeProfile(
        identity = MeIdentity(
            userName = "Ana Souza",
            firstName = "Ana",
            officialName = "Ana Souza",
            courseName = "Engenharia de Computação",
            enrollmentNumber = "synthetic-enrollment",
            username = "ana",
            avatarUrl = null,
        ),
        semester = null,
        enrollment = MeEnrollmentSummary(cr = null, crDelta = null, completedHours = 0, totalHours = 0),
        nextExam = null,
    )
}
