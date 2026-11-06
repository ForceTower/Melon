package dev.forcetower.unes.ui.feature.settings.passkeys

import android.app.Activity
import dev.forcetower.melon.core.analytics.NoOpAnalytics
import dev.forcetower.melon.core.common.Outcome
import dev.forcetower.melon.feature.auth.domain.model.PasskeyAttestation
import dev.forcetower.melon.feature.auth.domain.model.PasskeyCredential
import dev.forcetower.melon.feature.auth.domain.model.PasskeyError
import dev.forcetower.melon.feature.auth.domain.model.PasskeyRegistrationOptions
import dev.forcetower.melon.feature.auth.domain.usecase.DeletePasskeyUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.GetPasskeyRegistrationOptionsUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.ListPasskeysUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.RegisterPasskeyUseCase
import dev.forcetower.melon.feature.auth.domain.usecase.RenamePasskeyUseCase
import dev.forcetower.unes.ui.feature.onboarding.login.PasskeyClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

// A dismissed add or delete must not write its late result over whatever
// sheet the user has open by the time it lands.
@OptIn(ExperimentalCoroutinesApi::class)
internal class PasskeysViewModelTest {
    private val listPasskeys = mockk<ListPasskeysUseCase>()
    private val getRegistrationOptions = mockk<GetPasskeyRegistrationOptionsUseCase>()
    private val registerPasskey = mockk<RegisterPasskeyUseCase>()
    private val deletePasskey = mockk<DeletePasskeyUseCase>()
    private val passkeyClient = mockk<PasskeyClient>()
    private val activity = mockk<Activity>()
    private var keys = listOf(credential("a"), credential("b"))

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        coEvery { listPasskeys() } answers { Outcome.Ok(keys) }
        coEvery { getRegistrationOptions() } returns Outcome.Ok(PasskeyRegistrationOptions("{}"))
        coEvery { passkeyClient.register(any(), any(), any()) } returns attestation
        coEvery { registerPasskey(any(), any()) } returns Outcome.Ok(Unit)
    }

    @AfterTest
    fun tearDown() {
        unmockkAll()
        Dispatchers.resetMain()
    }

    @Test
    fun anAddDismissedWhileItsOptionsLoadNeverRaisesTheSystemSheet() =
        runTest {
            val options = CompletableDeferred<Outcome<PasskeyRegistrationOptions, PasskeyError>>()
            coEvery { getRegistrationOptions() } coAnswers { options.await() }
            val model = model()
            advanceUntilIdle()

            model.onIntent(PasskeysIntent.OpenAdd)
            model.onIntent(PasskeysIntent.ContinueAdd(activity))
            runCurrent()
            model.onIntent(PasskeysIntent.CloseSheet)
            options.complete(Outcome.Ok(PasskeyRegistrationOptions("{}")))
            advanceUntilIdle()

            coVerify(exactly = 0) { passkeyClient.register(any(), any(), any()) }
            assertEquals(PasskeySheet.None, model.state.value.sheet)
        }

    @Test
    fun aDismissedAddThatFailsLeavesTheDetailOpenedSince() =
        runTest {
            val options = CompletableDeferred<Outcome<PasskeyRegistrationOptions, PasskeyError>>()
            coEvery { getRegistrationOptions() } coAnswers { options.await() }
            val model = model()
            advanceUntilIdle()

            model.onIntent(PasskeysIntent.OpenAdd)
            model.onIntent(PasskeysIntent.ContinueAdd(activity))
            runCurrent()
            model.onIntent(PasskeysIntent.CloseSheet)
            model.onIntent(PasskeysIntent.OpenDetail("a"))
            options.complete(Outcome.Err(PasskeyError.NoConnection))
            advanceUntilIdle()

            assertEquals(PasskeySheet.Detail, model.state.value.sheet)
            assertEquals("a", model.state.value.detailId)
            assertNull(model.state.value.toast)
        }

    @Test
    fun aRegistrationLandingAfterTheSheetWasReopenedLeavesTheNewSheetOnItsFirstStep() =
        runTest {
            val registered = CompletableDeferred<Outcome<Unit, PasskeyError>>()
            coEvery { registerPasskey(any(), any()) } coAnswers { registered.await() }
            val model = model()
            advanceUntilIdle()

            model.onIntent(PasskeysIntent.OpenAdd)
            model.onIntent(PasskeysIntent.ContinueAdd(activity))
            runCurrent()
            model.onIntent(PasskeysIntent.CloseSheet)
            model.onIntent(PasskeysIntent.OpenAdd)
            registered.complete(Outcome.Ok(Unit))
            advanceUntilIdle()

            assertEquals(PasskeySheet.Add, model.state.value.sheet)
            assertEquals(AddStep.Choose, model.state.value.addStep)
        }

    @Test
    fun aSuccessStepDismissedEarlyStillListsTheNewKeyAndLeavesTheDetailOpenedSince() =
        runTest {
            val model = model()
            advanceUntilIdle()

            model.onIntent(PasskeysIntent.OpenAdd)
            keys = listOf(credential("new")) + keys
            model.onIntent(PasskeysIntent.ContinueAdd(activity))
            runCurrent()
            assertEquals(AddStep.Success, model.state.value.addStep)
            model.onIntent(PasskeysIntent.CloseSheet)
            model.onIntent(PasskeysIntent.OpenDetail("a"))
            advanceUntilIdle()

            assertEquals(PasskeySheet.Detail, model.state.value.sheet)
            assertEquals("a", model.state.value.detailId)
            assertEquals(listOf("new", "a", "b"), model.state.value.items.map { it.id })
        }

    @Test
    fun aDeleteInFlightKeepsItsDialogAndClosesOnlyItsOwnDetail() =
        runTest {
            val deleted = CompletableDeferred<Outcome<Unit, PasskeyError>>()
            coEvery { deletePasskey("a") } coAnswers { deleted.await() }
            val model = model()
            advanceUntilIdle()

            model.onIntent(PasskeysIntent.OpenDetail("a"))
            model.onIntent(PasskeysIntent.RequestDelete)
            model.onIntent(PasskeysIntent.ConfirmDelete)
            runCurrent()
            model.onIntent(PasskeysIntent.CancelDelete)
            assertEquals("a", model.state.value.confirmDeleteId)
            assertTrue(model.state.value.deleting)

            model.onIntent(PasskeysIntent.OpenDetail("b"))
            keys = listOf(credential("b"))
            deleted.complete(Outcome.Ok(Unit))
            runCurrent()

            assertEquals(PasskeySheet.Detail, model.state.value.sheet)
            assertEquals("b", model.state.value.detailId)
            assertNull(model.state.value.confirmDeleteId)
            assertFalse(model.state.value.deleting)
            assertEquals(PasskeyToast.Deleted, model.state.value.toast)
        }

    private fun model() =
        PasskeysViewModel(
            listPasskeys = listPasskeys,
            getRegistrationOptions = getRegistrationOptions,
            registerPasskey = registerPasskey,
            renamePasskey = mockk<RenamePasskeyUseCase>(),
            deletePasskey = deletePasskey,
            passkeyClient = passkeyClient,
            analytics = NoOpAnalytics,
        )

    private companion object {
        val attestation = PasskeyAttestation(
            id = "new",
            rawId = "new",
            authenticatorAttachment = "platform",
            clientDataJSON = "",
            attestationObject = "",
        )

        fun credential(id: String) =
            PasskeyCredential(id = id, deviceName = id, isSynced = true, createdAt = "2026-11-01T12:00:00Z")
    }
}
